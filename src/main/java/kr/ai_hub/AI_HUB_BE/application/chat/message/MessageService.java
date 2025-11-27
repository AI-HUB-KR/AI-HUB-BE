package kr.ai_hub.AI_HUB_BE.application.chat.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.ai_hub.AI_HUB_BE.application.chat.message.dto.*;
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
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Mono;
import org.springframework.core.ParameterizedTypeReference;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
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
    private final WebClient aiServerWebClient;
    private final ObjectMapper objectMapper;
    private final FileValidationService fileValidationService;

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

            // 2. User 메시지 저장 (별도 트랜잭션 서비스 이용)
            userMessage = messageTransactionService.saveUserMessage(context.chatRoom(), context.aiModel(), request);
            log.info("User 메시지 저장 완료: messageId={}", userMessage.getMessageId());

            // SSE 시작 알림
            emitter.send(SseEmitter.event().name("started").data("Message sending started"));

            // 3. 요청 바디 구성
            Map<String, Object> requestBody = buildRequestBody(request, context.aiModel());

            // 4. AI 서버로부터 SSE 스트리밍
            AiStreamingResult streamResult = streamAiResponse(requestBody, emitter);

            // 5. 응답 처리 (코인 계산 및 저장 - 별도 트랜잭션 서비스 이용)
            messageTransactionService.processCompletedResponse(
                    context.chatRoom(), context.aiModel(), context.user(),
                    userMessage, streamResult.aiResponseId(), streamResult.fullContent(),
                    streamResult.usage()
            );

            // 6. 완료 이벤트 전달
            sendCompletionEvent(emitter, userMessage, streamResult);
            log.info("메시지 전송 완료: roomId={}", roomId);

        } catch (JsonProcessingException e) {
            // AI 응답 JSON 파싱 실패
            log.error("AI 응답 JSON 파싱 실패: {}", e.getMessage(), e);
            handleMessageError(userMessage, new AIServerException("AI 응답 형식이 유효하지 않습니다", e), emitter);

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
     * 완료 이벤트를 클라이언트에게 전송합니다.
     */
    private void sendCompletionEvent(SseEmitter emitter, Message userMessage, AiStreamingResult result)
            throws IOException {
        Map<String, Object> completedData = new HashMap<>();
        completedData.put("userMessageId", userMessage.getMessageId());
        completedData.put("aiResponseId", result.aiResponseId());
        completedData.put("inputTokens", result.usage().inputTokens());
        completedData.put("outputTokens", result.usage().outputTokens());
        emitter.send(SseEmitter.event().name("completed").data(completedData));
        emitter.complete();
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
     * AI 서버로부터 SSE 스트리밍 응답을 처리합니다.
     */
    private AiStreamingResult streamAiResponse(Map<String, Object> requestBody, SseEmitter emitter)
            throws JsonProcessingException, IOException, IllegalStateException {
        String aiResponseId = null;
        StringBuilder fullContent = new StringBuilder();
        AiUsage usage = null;

        var stream = aiServerWebClient.post()
                .uri("/ai/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .toStream();

        for (String line : (Iterable<String>) stream::iterator) {
            if (line == null || line.isBlank()) {
                continue;
            }
            SseEvent event = objectMapper.readValue(line, SseEvent.class);

            switch (event.type()) {
                case "response.created":
                    log.info("AI 서버 response.created 이벤트 수신");
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
                    log.info("AI 서버 response.completed 이벤트 수신");
                    if (event.response() != null) {
                        usage = event.response().usage();
                        if (usage != null) {
                            log.info("AI 응답 완료: tokens={}", usage.totalTokens());
                        } else {
                            log.error("usage가 null입니다! response={}", event.response());
                        }
                    } else {
                        log.error("response 객체가 null입니다!");
                    }
                    break;

                case "error":
                    log.info("AI 서버 error 이벤트 수신");
                    if (event.error() != null) {
                        log.error("AI 서버 에러: {}", event.error().message());
                        throw new AIServerException(event.error().message());
                    }
                    break;
            }
        }

        return new AiStreamingResult(aiResponseId, fullContent.toString(), usage);
    }

    /**
     * AI 서버로 전송할 요청 바디를 구성합니다.
     */
    private Map<String, Object> buildRequestBody(SendMessageRequest request, AIModel aiModel) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("message", request.message());
        requestBody.put("model", aiModel.getModelName());

        if (request.fileId() != null) {
            requestBody.put("file_id", request.fileId());
        }
        if (request.previousResponseId() != null) {
            requestBody.put("previous_response_id", request.previousResponseId());
        }

        return requestBody;
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
        log.info("파일 업로드 시작: fileName={}, size={}, modelId={}",
                file.getOriginalFilename(), file.getSize(), modelId);

        // 파일 검증
        fileValidationService.validateFile(file);

        // AI 모델 조회
        AIModel aiModel = aiModelRepository.findById(modelId)
                .orElseThrow(() -> new ModelNotFoundException("AI 모델을 찾을 수 없습니다: " + modelId));
        log.debug("AI 모델 조회 성공: modelName={}", aiModel.getModelName());

        // AI 서버에 파일 업로드
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
                            clientResponse -> clientResponse.bodyToMono(
                                    new ParameterizedTypeReference<AiServerResponse<AiUploadData>>() {})
                                    .flatMap(errorResponse -> {
                                        String errorMessage = errorResponse.error() != null
                                                ? errorResponse.error().message()
                                                : "AI 서버 응답 에러";
                                        log.error("AI 서버 파일 업로드 실패: {}", errorMessage);
                                        return Mono.error(new AIServerException(errorMessage));
                                    })
                    )
                    .bodyToMono(new ParameterizedTypeReference<AiServerResponse<AiUploadData>>() {})
                    .block(Duration.ofSeconds(30));  // 30초 타임아웃 명시

            if (response == null || !response.success() || response.data() == null) {
                log.error("AI 서버 응답 없음 또는 실패");
                throw new AIServerException("AI 서버로부터 응답을 받지 못했습니다");
            }

            AiUploadData uploadData = response.data();
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
     * 메시지 요청 검증 결과를 담는 DTO
     */
    private record ValidatedMessageContext(
            User user,
            ChatRoom chatRoom,
            AIModel aiModel,
            UserWallet wallet
    ) {}

    /**
     * AI 서버 SSE 스트리밍 결과를 담는 DTO
     */
    private record AiStreamingResult(
            String aiResponseId,
            String fullContent,
            AiUsage usage
    ) {}
}
