package kr.ai_hub.AI_HUB_BE.application.message;

import kr.ai_hub.AI_HUB_BE.application.message.dto.*;
import kr.ai_hub.AI_HUB_BE.domain.aimodel.entity.AIModel;
import kr.ai_hub.AI_HUB_BE.domain.aimodel.repository.AIModelRepository;
import kr.ai_hub.AI_HUB_BE.domain.chatroom.entity.ChatRoom;
import kr.ai_hub.AI_HUB_BE.domain.chatroom.repository.ChatRoomRepository;
import kr.ai_hub.AI_HUB_BE.domain.message.entity.Message;
import kr.ai_hub.AI_HUB_BE.domain.message.repository.MessageRepository;
import kr.ai_hub.AI_HUB_BE.domain.user.repository.UserRepository;
import kr.ai_hub.AI_HUB_BE.global.auth.SecurityContextHelper;
import kr.ai_hub.AI_HUB_BE.global.error.exception.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
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
    private final SecurityContextHelper securityContextHelper;
    private final WebClient aiServerWebClient;

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
                    .block();

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
