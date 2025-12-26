package kr.ai_hub.AI_HUB_BE.application.chat.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.ai_hub.AI_HUB_BE.application.chat.message.dto.AiUsage;
import kr.ai_hub.AI_HUB_BE.application.chat.message.dto.FileAttachment;
import kr.ai_hub.AI_HUB_BE.application.chat.message.dto.SendMessageRequest;
import kr.ai_hub.AI_HUB_BE.domain.aimodel.AIModel;
import kr.ai_hub.AI_HUB_BE.domain.chat.ChatRoom;
import kr.ai_hub.AI_HUB_BE.domain.chat.ChatRoomRepository;
import kr.ai_hub.AI_HUB_BE.domain.chat.Message;
import kr.ai_hub.AI_HUB_BE.domain.chat.MessageRepository;
import kr.ai_hub.AI_HUB_BE.domain.chat.MessageRole;
import kr.ai_hub.AI_HUB_BE.domain.payment.CoinTransaction;
import kr.ai_hub.AI_HUB_BE.domain.payment.CoinTransactionRepository;
import kr.ai_hub.AI_HUB_BE.domain.user.User;
import kr.ai_hub.AI_HUB_BE.domain.user.UserWallet;
import kr.ai_hub.AI_HUB_BE.domain.user.UserWalletRepository;
import kr.ai_hub.AI_HUB_BE.global.error.exception.WalletNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageTransactionService {

    private final MessageRepository messageRepository;
    private final UserWalletRepository userWalletRepository;
    private final CoinTransactionRepository coinTransactionRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ObjectMapper objectMapper;

    /**
     * User 메시지를 저장합니다 (별도 트랜잭션).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Message saveUserMessage(ChatRoom chatRoom, AIModel aiModel, SendMessageRequest request) {
        // files 배열을 JSON 문자열로 변환하여 저장
        String fileUrlJson = convertFilesToJson(request.files());

        Message userMessage = Message.builder()
                .chatRoom(chatRoom)
                .role(MessageRole.USER)
                .content(request.message())
                .fileUrl(fileUrlJson)
                .aiModel(aiModel)
                .build();

        return messageRepository.save(userMessage);
    }

    /**
     * FileAttachment 목록을 JSON 문자열로 변환합니다.
     *
     * @param files 파일 첨부 목록
     * @return JSON 문자열 또는 null (파일이 없는 경우)
     */
    private String convertFilesToJson(List<FileAttachment> files) {
        if (files == null || files.isEmpty()) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(files);
        } catch (JsonProcessingException e) {
            log.error("파일 정보 JSON 변환 실패: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * AI 응답 완료 후 코인 차감 및 메시지 저장을 처리합니다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processCompletedResponse(
            ChatRoom chatRoom, AIModel aiModel, User user,
            Message userMessage, String aiResponseId, String fullContent, AiUsage usage) {

        // 코인 계산
        BigDecimal inputCoin = calculateCoin(usage.inputTokens(), aiModel.getInputPricePer1m());
        BigDecimal outputCoin = calculateCoin(usage.outputTokens(), aiModel.getOutputPricePer1m());
        BigDecimal totalCoin = inputCoin.add(outputCoin);

        log.info("코인 계산: input={}, output={}, total={}", inputCoin, outputCoin, totalCoin);

        // 지갑 다시 조회 (영속 상태 보장 및 데이터 정합성 확보)
        UserWallet wallet = userWalletRepository.findByUserUserId(user.getUserId())
                .orElseThrow(() -> new WalletNotFoundException("지갑을 찾을 수 없습니다"));

        // 코인 차감 (별도 트랜잭션 - 롤백 안 됨)
        deductBalance(user.getUserId(), totalCoin);

        // Assistant 메시지 저장
        Message assistantMessage = Message.builder()
                .chatRoom(chatRoom)
                .role(MessageRole.ASSISTANT)
                .content(fullContent)
                .aiModel(aiModel)
                .tokenCount(BigDecimal.valueOf(usage.outputTokens()))
                .coinCount(outputCoin)
                .responseId(aiResponseId)
                .build();
        messageRepository.save(assistantMessage);

        // User 메시지 업데이트
        userMessage.updateResponseId(aiResponseId);
        userMessage.updateTokenAndCoin(
                BigDecimal.valueOf(usage.inputTokens()),
                inputCoin
        );
        messageRepository.save(userMessage);

        // ChatRoom 코인 사용량 업데이트 (영속 상태 보장 및 데이터 정합성 확보)
        ChatRoom managedChatRoom = chatRoomRepository.findById(chatRoom.getRoomId())
                .orElseThrow(() -> new IllegalStateException("채팅방을 찾을 수 없습니다"));

        managedChatRoom.addCoinUsage(totalCoin);

        // CoinTransaction 기록
        CoinTransaction transaction = CoinTransaction.builder()
                .user(user)
                .chatRoom(managedChatRoom)
                .message(assistantMessage)
                .transactionType("AI_USAGE")
                .coinUsage(totalCoin.negate()) // 차감이므로 음수
                .balanceAfter(wallet.getBalance())
                .description(String.format("AI 모델 사용: %s (입력: %d토큰, 출력: %d토큰)",
                        aiModel.getModelName(), usage.inputTokens(), usage.outputTokens()))
                .aiModel(aiModel)
                .build();
        coinTransactionRepository.save(transaction);

        log.info("코인 차감 및 메시지 저장 완료: totalCoin={}, balance={}", totalCoin, wallet.getBalance());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deductBalance(Integer userId, BigDecimal amount) {
        UserWallet wallet = userWalletRepository.findByUserUserId(userId)
                .orElseThrow(() -> new WalletNotFoundException("지갑을 찾을 수 없습니다"));

        wallet.deductBalance(amount);

    }

    /**
     * User 메시지를 삭제합니다 (보상 트랜잭션).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteUserMessage(Message userMessage) {
        // 다시 조회해서 삭제해야 안전함 (Detached 상태일 수 있음)
        if (userMessage.getMessageId() != null) {
            messageRepository.findById(userMessage.getMessageId())
                    .ifPresent(messageRepository::delete);
        }
        log.debug("User 메시지 삭제: messageId={}", userMessage.getMessageId());
    }

    /**
     * 토큰량으로부터 코인을 계산합니다.
     */
    private BigDecimal calculateCoin(Integer tokens, BigDecimal pricePer1M) {
        if (tokens == null || tokens == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal tokenAmount = BigDecimal.valueOf(tokens);
        BigDecimal oneMillium = BigDecimal.valueOf(1_000_000);

        return tokenAmount.divide(oneMillium, 10, RoundingMode.HALF_UP)
                .multiply(pricePer1M)
                .setScale(10, RoundingMode.HALF_UP);
    }
}
