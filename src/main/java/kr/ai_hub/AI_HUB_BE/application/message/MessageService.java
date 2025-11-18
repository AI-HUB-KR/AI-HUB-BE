package kr.ai_hub.AI_HUB_BE.application.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.ai_hub.AI_HUB_BE.application.message.dto.*;
import kr.ai_hub.AI_HUB_BE.domain.aimodel.entity.AIModel;
import kr.ai_hub.AI_HUB_BE.domain.aimodel.repository.AIModelRepository;
import kr.ai_hub.AI_HUB_BE.domain.chatroom.entity.ChatRoom;
import kr.ai_hub.AI_HUB_BE.domain.chatroom.repository.ChatRoomRepository;
import kr.ai_hub.AI_HUB_BE.domain.cointransaction.entity.CoinTransaction;
import kr.ai_hub.AI_HUB_BE.domain.cointransaction.repository.CoinTransactionRepository;
import kr.ai_hub.AI_HUB_BE.domain.message.entity.Message;
import kr.ai_hub.AI_HUB_BE.domain.message.entity.MessageRole;
import kr.ai_hub.AI_HUB_BE.domain.message.repository.MessageRepository;
import kr.ai_hub.AI_HUB_BE.domain.user.entity.User;
import kr.ai_hub.AI_HUB_BE.domain.user.repository.UserRepository;
import kr.ai_hub.AI_HUB_BE.domain.userwallet.entity.UserWallet;
import kr.ai_hub.AI_HUB_BE.domain.userwallet.repository.UserWalletRepository;
import kr.ai_hub.AI_HUB_BE.global.auth.SecurityContextHelper;
import kr.ai_hub.AI_HUB_BE.global.error.exception.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MessageService {

    private final MessageRepository messageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final AIModelRepository aiModelRepository;
    private final UserWalletRepository userWalletRepository;
    private final CoinTransactionRepository coinTransactionRepository;
    private final SecurityContextHelper securityContextHelper;
    private final WebClient aiServerWebClient;
    private final ObjectMapper objectMapper;

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "webp");
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    /**
     * 특정 채팅방의 메시지 목록을 페이지네이션하여 조회합니다.
     */
    public Page<MessageListItemResponse> getMessages(UUID roomId, Pageable pageable) {
        Integer userId = securityContextHelper.getCurrentUserId();
        log.debug("채팅방 {} 메시지 목록 조회 by 사용자 {} (page={}, size={})",
                roomId, userId, pageable.getPageNumber(), pageable.getPageSize());

        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("채팅방을 찾을 수 없습니다: " + roomId));

        // 권한 확인: 채팅방 소유자만 조회 가능
        if (!chatRoom.getUser().getUserId().equals(userId)) {
            log.warn("메시지 목록 접근 권한 없음: roomId={}, userId={}", roomId, userId);
            throw new ForbiddenException("해당 채팅방의 메시지에 접근할 권한이 없습니다");
        }

        Page<Message> messages = messageRepository.findByChatRoom(chatRoom, pageable);

        return messages.map(MessageListItemResponse::from);
    }

    /**
     * 특정 메시지의 상세 정보를 조회합니다.
     */
    public MessageResponse getMessage(UUID messageId) {
        Integer userId = securityContextHelper.getCurrentUserId();
        log.debug("메시지 {} 상세 조회 by 사용자 {}", messageId, userId);

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new MessageNotFoundException("메시지를 찾을 수 없습니다: " + messageId));

        // 권한 확인: 메시지가 속한 채팅방의 소유자만 조회 가능
        if (!message.getChatRoom().getUser().getUserId().equals(userId)) {
            log.warn("메시지 접근 권한 없음: messageId={}, userId={}", messageId, userId);
            throw new ForbiddenException("해당 메시지에 접근할 권한이 없습니다");
        }

        return MessageResponse.from(message);
    }

    /**
     * 메시지를 전송하고 AI 응답을 SSE로 스트리밍합니다.
     * Virtual Threads가 I/O 블로킹을 자동으로 처리합니다.
     *
     * @param roomId  채팅방 ID
     * @param request 메시지 전송 요청
     * @param emitter SSE Emitter
     */
    public void sendMessage(UUID roomId, SendMessageRequest request, SseEmitter emitter) {
        try {
            log.info("메시지 전송 시작: roomId={}, modelId={}", roomId, request.modelId());

            // 1. 사전 검증
            Integer userId = securityContextHelper.getCurrentUserId();
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + userId));

            ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                    .orElseThrow(() -> new RoomNotFoundException("채팅방을 찾을 수 없습니다: " + roomId));

            // 권한 확인
            if (!chatRoom.getUser().getUserId().equals(userId)) {
                log.warn("채팅방 접근 권한 없음: roomId={}, userId={}", roomId, userId);
                emitter.completeWithError(new ForbiddenException("해당 채팅방에 접근할 권한이 없습니다"));
                return;
            }

            AIModel aiModel = aiModelRepository.findById(request.modelId())
                    .orElseThrow(() -> new ModelNotFoundException("AI 모델을 찾을 수 없습니다: " + request.modelId()));

            UserWallet wallet = userWalletRepository.findByUser(user)
                    .orElseThrow(() -> new WalletNotFoundException("지갑을 찾을 수 없습니다"));

            // 잔고 검증
            if (wallet.getBalance().compareTo(BigDecimal.ZERO) < 0) {
                log.warn("코인 잔액이 음수입니다: userId={}, balance={}", userId, wallet.getBalance());
                emitter.completeWithError(new InsufficientBalanceException("코인 잔액이 부족합니다"));
                return;
            }

            // 2. User 메시지 저장 (별도 트랜잭션)
            Message userMessage = saveUserMessage(chatRoom, aiModel, request);
            log.info("User 메시지 저장 완료: messageId={}", userMessage.getMessageId());

            // SSE 시작 알림
            emitter.send(SseEmitter.event().name("started").data("Message sending started"));

            // 3. AI 서버 SSE 스트리밍 (동기 방식)
            String aiResponseId = null;
            StringBuilder fullContent = new StringBuilder();
            AiUsage usage = null;

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("message", request.message());
            requestBody.put("model", aiModel.getModelName());
            if (request.fileId() != null) {
                requestBody.put("file_id", request.fileId());
            }
            if (request.previousResponseId() != null) {
                requestBody.put("previous_response_id", request.previousResponseId());
            }

            // AI 서버와 SSE 통신 (blocking stream으로 변환)
            try {
                var stream = aiServerWebClient.post()
                        .uri("/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(requestBody)
                        .retrieve()
                        .bodyToFlux(String.class)
                        .toStream();  // Flux를 blocking Stream으로 변환

                // Stream을 순회하며 SSE 이벤트 처리
                for (String line : (Iterable<String>) stream::iterator) {
                    if (line.startsWith("data: ")) {
                        String jsonData = line.substring(6);
                        SseEvent event = objectMapper.readValue(jsonData, SseEvent.class);

                        switch (event.type()) {
                            case "response.created":
                                if (event.response() != null) {
                                    aiResponseId = event.response().id();
                                    log.debug("AI 응답 생성: id={}", aiResponseId);
                                }
                                break;

                            case "response.output_text.delta":
                                if (event.delta() != null) {
                                    fullContent.append(event.delta());
                                    // 클라이언트에게 delta 전달
                                    emitter.send(SseEmitter.event()
                                            .name("delta")
                                            .data(event.delta()));
                                }
                                break;

                            case "response.completed":
                                if (event.response() != null) {
                                    usage = event.response().usage();
                                    log.info("AI 응답 완료: tokens={}", usage.totalTokens());
                                }
                                break;

                            case "error":
                                if (event.error() != null) {
                                    log.error("AI 서버 에러: {}", event.error().message());
                                    throw new AIServerException(event.error().message());
                                }
                                break;
                        }
                    }
                }

                // 4. 코인 계산 및 차감, 메시지 저장
                processCompletedResponse(
                        chatRoom, aiModel, user, wallet, userMessage,
                        aiResponseId, fullContent.toString(), usage
                );

                // 5. 완료 이벤트 전달
                Map<String, Object> completedData = new HashMap<>();
                completedData.put("userMessageId", userMessage.getMessageId());
                completedData.put("aiResponseId", aiResponseId);
                completedData.put("inputTokens", usage.inputTokens());
                completedData.put("outputTokens", usage.outputTokens());
                emitter.send(SseEmitter.event().name("completed").data(completedData));
                emitter.complete();

                log.info("메시지 전송 완료: roomId={}", roomId);

            } catch (IOException e) {
                // SSE 통신 에러 (네트워크, 연결 실패 등)
                log.error("SSE 통신 에러: {}", e.getMessage(), e);
                throw new AIServerException("SSE 연결 실패", e);
            } catch (JsonProcessingException e) {
                // AI 응답 JSON 파싱 실패
                log.error("AI 응답 JSON 파싱 실패: {}", e.getMessage(), e);
                throw new AIServerException("AI 응답 형식이 유효하지 않습니다", e);
            } catch (IllegalStateException e) {
                // Stream 변환 실패
                log.error("스트림 변환 실패: {}", e.getMessage(), e);
                throw new AIServerException("스트림 처리 중 오류가 발생했습니다", e);
            } catch (Exception e) {
                // 예상치 못한 에러
                log.error("예상치 못한 에러: {}", e.getMessage(), e);
                throw new AIServerException("메시지 전송 중 에러가 발생했습니다", e);
            }

        } catch (Exception e) {
            log.error("메시지 전송 중 에러: {}", e.getMessage(), e);

            // AI 통신 실패 시 User 메시지 삭제 (보상 트랜잭션)
            if (userMessage != null) {
                try {
                    deleteUserMessage(userMessage);
                    log.info("AI 통신 실패로 User 메시지 삭제 완료: messageId={}", userMessage.getMessageId());
                } catch (Exception deleteError) {
                    log.error("User 메시지 삭제 실패: messageId={}, error={}",
                            userMessage.getMessageId(), deleteError.getMessage(), deleteError);
                }
            }

            emitter.completeWithError(e);
        }
    }

    /**
     * User 메시지를 저장합니다 (별도 트랜잭션).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected Message saveUserMessage(ChatRoom chatRoom, AIModel aiModel, SendMessageRequest request) {
        Message userMessage = Message.builder()
                .chatRoom(chatRoom)
                .role(MessageRole.USER)
                .content(request.message())
                .fileUrl(request.fileId())
                .aiModel(aiModel)
                .build();

        return messageRepository.save(userMessage);
    }

    /**
     * User 메시지를 삭제합니다 (보상 트랜잭션).
     * AI 통신 실패 시 orphaned User 메시지를 제거하기 위해 사용됩니다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void deleteUserMessage(Message userMessage) {
        messageRepository.delete(userMessage);
        log.debug("User 메시지 삭제: messageId={}", userMessage.getMessageId());
    }

    /**
     * AI 응답 완료 후 코인 차감 및 메시지 저장을 처리합니다.
     */
    @Transactional
    protected void processCompletedResponse(
            ChatRoom chatRoom, AIModel aiModel, User user, UserWallet wallet,
            Message userMessage, String aiResponseId, String fullContent, AiUsage usage) {

        // 코인 계산
        BigDecimal inputCoin = calculateCoin(usage.inputTokens(), aiModel.getInputPricePer1m());
        BigDecimal outputCoin = calculateCoin(usage.outputTokens(), aiModel.getOutputPricePer1m());
        BigDecimal totalCoin = inputCoin.add(outputCoin);

        log.info("코인 계산: input={}, output={}, total={}", inputCoin, outputCoin, totalCoin);

        // 코인 차감
        wallet.deductBalance(totalCoin);

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

        // ChatRoom 코인 사용량 업데이트
        chatRoom.addCoinUsage(totalCoin);

        // CoinTransaction 기록
        CoinTransaction transaction = CoinTransaction.builder()
                .user(user)
                .chatRoom(chatRoom)
                .message(assistantMessage)
                .transactionType("AI_USAGE")
                .amount(totalCoin.negate()) // 차감이므로 음수
                .balanceAfter(wallet.getBalance())
                .description(String.format("AI 모델 사용: %s (입력: %d토큰, 출력: %d토큰)",
                        aiModel.getModelName(), usage.inputTokens(), usage.outputTokens()))
                .aiModel(aiModel)
                .build();
        coinTransactionRepository.save(transaction);

        log.info("코인 차감 및 메시지 저장 완료: totalCoin={}, balance={}", totalCoin, wallet.getBalance());
    }

    /**
     * 토큰량으로부터 코인을 계산합니다.
     * 공식: (토큰량 / 1,000,000) * 모델_가격_per_1M
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

    /**
     * 파일을 AI 서버에 업로드합니다.
     *
     * @param file    업로드할 파일
     * @param modelId AI 모델 ID
     * @return 파일 업로드 응답 (file ID)
     */
    public FileUploadResponse uploadFile(MultipartFile file, Integer modelId) {
        log.info("파일 업로드 시작: fileName={}, size={}, modelId={}",
                file.getOriginalFilename(), file.getSize(), modelId);

        // 1. 파일 검증
        validateFile(file);

        // 2. AI 모델 조회
        AIModel aiModel = aiModelRepository.findById(modelId)
                .orElseThrow(() -> new ModelNotFoundException("AI 모델을 찾을 수 없습니다: " + modelId));
        log.debug("AI 모델 조회 성공: modelName={}", aiModel.getModelName());

        // 3. AI 서버에 파일 업로드
        try {
            // MultipartBodyBuilder를 사용하여 multipart/form-data 요청 생성
            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("file", file.getResource());

            // AI 서버에 POST 요청
            AiServerResponse<AiUploadData> response = aiServerWebClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/ai/upload")
                            .queryParam("model", aiModel.getModelName())
                            .build())
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .retrieve()
                    .onStatus(
                            status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> clientResponse.bodyToMono(AiServerResponse.class)
                                    .flatMap(errorResponse -> {
                                        String errorMessage = errorResponse.error() != null
                                                ? errorResponse.error().message()
                                                : "AI 서버 응답 에러";
                                        log.error("AI 서버 파일 업로드 실패: {}", errorMessage);
                                        return Mono.error(new AIServerException(errorMessage));
                                    })
                    )
                    .bodyToMono(AiServerResponse.class)
                    .cast(AiServerResponse.class)
                    .block(Duration.ofSeconds(30));  // 30초 타임아웃 명시

            if (response == null || !response.success() || response.data() == null) {
                log.error("AI 서버 응답 없음 또는 실패");
                throw new AIServerException("AI 서버로부터 응답을 받지 못했습니다");
            }

            AiUploadData uploadData = (AiUploadData) response.data();
            String fileId = uploadData.fileId();
            log.info("파일 업로드 성공: fileId={}", fileId);

            return FileUploadResponse.of(fileId);

        } catch (Exception e) {
            log.error("파일 업로드 중 에러 발생: {}", e.getMessage(), e);
            if (e instanceof AIServerException) {
                throw e;
            }
            throw new AIServerException("파일 업로드 중 에러가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 파일 유효성 검증
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("파일이 제공되지 않았습니다");
        }

        // 파일 크기 검증
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ValidationException(
                    String.format("파일 크기가 너무 큽니다. 최대 크기: %dMB", MAX_FILE_SIZE / 1024 / 1024));
        }

        // 파일 확장자 검증
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new ValidationException("파일명이 유효하지 않습니다");
        }

        String extension = getFileExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ValidationException(
                    String.format("지원하지 않는 파일 형식입니다: %s. 지원되는 형식: %s",
                            extension, String.join(", ", ALLOWED_EXTENSIONS)));
        }
    }

    /**
     * 파일 확장자 추출
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1).toLowerCase();
    }
}
