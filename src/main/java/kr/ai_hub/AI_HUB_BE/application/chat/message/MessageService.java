package kr.ai_hub.AI_HUB_BE.application.chat.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import kr.ai_hub.AI_HUB_BE.application.chat.message.dto.AiStreamingResult;
import kr.ai_hub.AI_HUB_BE.application.chat.message.dto.FileUploadResponse;
import kr.ai_hub.AI_HUB_BE.application.chat.message.dto.MessageListItemResponse;
import kr.ai_hub.AI_HUB_BE.application.chat.message.dto.MessageResponse;
import kr.ai_hub.AI_HUB_BE.application.chat.message.dto.SendMessageRequest;
import kr.ai_hub.AI_HUB_BE.domain.aimodel.AIModel;
import kr.ai_hub.AI_HUB_BE.domain.aimodel.AIModelRepository;
import kr.ai_hub.AI_HUB_BE.domain.chat.ChatRoom;
import kr.ai_hub.AI_HUB_BE.domain.chat.ChatRoomRepository;
import kr.ai_hub.AI_HUB_BE.domain.chat.Message;
import kr.ai_hub.AI_HUB_BE.domain.chat.MessageRepository;
import kr.ai_hub.AI_HUB_BE.domain.user.User;
import kr.ai_hub.AI_HUB_BE.domain.user.UserRepository;
import kr.ai_hub.AI_HUB_BE.domain.user.UserWallet;
import kr.ai_hub.AI_HUB_BE.domain.user.UserWalletRepository;
import kr.ai_hub.AI_HUB_BE.global.auth.SecurityContextHelper;
import kr.ai_hub.AI_HUB_BE.global.error.exception.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
    private final MessageTransactionService messageTransactionService;
    private final SecurityContextHelper securityContextHelper;
    private final MessageRequestBuilder messageRequestBuilder;
    private final AiSseHandler aiSseHandler;
    private final FileUploadService fileUploadService;

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
     * 리팩토링된 오케스트레이션 메서드 - 각 단계별 책임을 분리된 메서드에 위임합니다.
     *
     * @param roomId  채팅방 ID
     * @param request 메시지 전송 요청
     * @param emitter SSE Emitter
     */
    public void sendMessage(UUID roomId, SendMessageRequest request, SseEmitter emitter) {
        Message userMessage = null;

        try {
            log.info("메시지 전송 시작: roomId={}, modelId={}", roomId, request.modelId());

            // 1. 요청 검증 및 리소스 조회
            ValidatedMessageContext context = validateMessageRequest(roomId, request);

            // SSE 시작 알림
            emitter.send(SseEmitter.event().name("started").data("Message sending started"));

            // 2. 요청 바디 구성 (이전 채팅 히스토리 포함)
            Map<String, Object> requestBody = messageRequestBuilder.build(request, context.aiModel(), context.chatRoom());

            // 3. User 메시지 저장 (별도 트랜잭션 서비스 이용)
            userMessage = messageTransactionService.saveUserMessage(context.chatRoom(), context.aiModel(), request);
            log.info("User 메시지 저장 완료: messageId={}", userMessage.getMessageId());

            // 4. AI 서버로부터 SSE 스트리밍
            AiStreamingResult streamResult = aiSseHandler.stream(requestBody, emitter);

            // 5. 응답 처리 (코인 계산 및 저장 - 별도 트랜잭션 서비스 이용)
            messageTransactionService.processCompletedResponse(
                    context.chatRoom(), context.aiModel(), context.user(),
                    userMessage, streamResult.aiResponseId(), streamResult.fullContent(),
                    streamResult.usage()
            );

            // 6. 완료 이벤트 전달 및 Emitter 종료
            emitter.send(SseEmitter.event().name("usage").data(streamResult));
            emitter.complete();

            log.info("메시지 전송 완료: roomId={}", roomId);

        } catch (JsonProcessingException e) {
            // AI 응답 JSON 파싱 실패
            log.error("AI 응답 JSON 파싱 실패: {}", e.getMessage(), e);
            handleMessageError(userMessage, new AIServerException("AI 응답 형식이 유효하지 않습니다", e), emitter);
        } catch (AIServerException e) {
            log.error("AI 서버 응답 에러: {}", e.getMessage(), e);
            handleMessageError(userMessage, e, emitter);
        } catch (IOException e) {
            // SSE 통신 에러
            log.error("SSE 통신 에러: {}", e.getMessage(), e);
            handleMessageError(userMessage, new AIServerException("SSE 연결 실패", e), emitter);

        } catch (IllegalStateException e) {
            // Stream 변환 실패
            log.error("스트림 변환 실패: {}", e.getMessage(), e);
            handleMessageError(userMessage, new AIServerException("스트림 처리 중 오류가 발생했습니다", e), emitter);

        } catch (Exception e) {
            // 예상치 못한 에러
            log.error("예상치 못한 에러: {}", e.getMessage(), e);
            handleMessageError(userMessage, e, emitter);
        }
    }

    /**
     * 메시지 전송 실패 시 에러를 처리합니다 (보상 트랜잭션).
     */
    private void handleMessageError(Message userMessage, Exception error, SseEmitter emitter) {
        log.error("메시지 전송 중 에러: {}", error.getMessage(), error);

        // AI 통신 실패 시 User 메시지 삭제 (보상 트랜잭션)
        if (userMessage != null) {
            try {
                messageTransactionService.deleteUserMessage(userMessage);
                log.info("AI 통신 실패로 User 메시지 삭제 완료: messageId={}", userMessage.getMessageId());
            } catch (Exception deleteError) {
                log.error("User 메시지 삭제 실패: messageId={}, error={}",
                        userMessage.getMessageId(), deleteError.getMessage(), deleteError);
            }
        }

        emitter.completeWithError(error);
    }

    /**
     * 메시지 전송 요청을 검증하고 필요한 리소스를 조회합니다.
     */
    private ValidatedMessageContext validateMessageRequest(UUID roomId, SendMessageRequest request) {
        Integer userId = securityContextHelper.getCurrentUserId();

        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + userId));

        // 채팅방 조회
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("채팅방을 찾을 수 없습니다: " + roomId));

        // 채팅방 권한 확인
        if (!chatRoom.getUser().getUserId().equals(userId)) {
            log.warn("채팅방 접근 권한 없음: roomId={}, userId={}", roomId, userId);
            throw new ForbiddenException("해당 채팅방에 접근할 권한이 없습니다");
        }

        // AI 모델 조회
        AIModel aiModel = aiModelRepository.findById(request.modelId())
                .orElseThrow(() -> new ModelNotFoundException("AI 모델을 찾을 수 없습니다: " + request.modelId()));

        // 지갑 조회 및 잔고 검증
        UserWallet wallet = userWalletRepository.findByUser(user)
                .orElseThrow(() -> new WalletNotFoundException("지갑을 찾을 수 없습니다"));

        if (wallet.getBalance().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("코인 잔액이 0 이하 입니다: userId={}, balance={}", userId, wallet.getBalance());
            throw new InsufficientBalanceException("코인 잔액이 부족합니다");
        }

        return new ValidatedMessageContext(user, chatRoom, aiModel, wallet);
    }

    /**
     * 파일을 AI 서버에 업로드합니다.
     *
     * @param file    업로드할 파일
     * @param modelId AI 모델 ID
     * @return 파일 업로드 응답 (file ID)
     */
    public FileUploadResponse uploadFile(MultipartFile file, Integer modelId) {
        return fileUploadService.uploadFile(file, modelId);
    }


    /**
     * 메시지 요청 검증 결과를 담는 DTO
     */
    private record ValidatedMessageContext(
            User user,
            ChatRoom chatRoom,
            AIModel aiModel,
            UserWallet wallet
    ) {
    }

}
