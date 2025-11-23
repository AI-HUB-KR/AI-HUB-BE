package kr.ai_hub.AI_HUB_BE.controller.chat;

import jakarta.validation.Valid;
import kr.ai_hub.AI_HUB_BE.application.chat.message.MessageService;
import kr.ai_hub.AI_HUB_BE.application.chat.message.dto.FileUploadResponse;
import kr.ai_hub.AI_HUB_BE.application.chat.message.dto.MessageListItemResponse;
import kr.ai_hub.AI_HUB_BE.application.chat.message.dto.MessageResponse;
import kr.ai_hub.AI_HUB_BE.application.chat.message.dto.SendMessageRequest;
import kr.ai_hub.AI_HUB_BE.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.concurrent.DelegatingSecurityContextRunnable;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
public class ChatMessageController {

    private final MessageService messageService;

    /**
     * 메시지 전송 및 SSE 스트리밍 API
     * <p>
     * 사용자 메시지를 AI 서버로 전송하고, AI 응답을 SSE 스트리밍으로 반환합니다.
     * - User 메시지 저장
     * - AI 서버와 SSE 통신
     * - 실시간 증분 텍스트 전달
     * - 코인 차감 및 Assistant 메시지 저장
     * </p>
     */
    @PostMapping(value = "/send/{roomId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sendMessage(
            @PathVariable UUID roomId,
            @Valid @RequestBody SendMessageRequest request) {
        log.info("메시지 전송 API 호출: roomId={}, modelId={}", roomId, request.modelId());

        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L); // 5분 타임아웃

        // SecurityContext를 명시적으로 비동기 작업에 전파
        Runnable task = () -> messageService.sendMessage(roomId, request, emitter);
        Runnable securityContextTask = new DelegatingSecurityContextRunnable(task, SecurityContextHolder.getContext());
        Thread.ofVirtual().start(securityContextTask);

        return emitter;
    }

    /**
     * 파일 업로드 API
     * <p>
     * 이미지 파일을 AI 서버에 업로드하고 file ID를 반환합니다.
     * 이 ID는 이후 메시지 전송 시 사용할 수 있습니다.
     * </p>
     */
    @PostMapping(value = "/files/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadFile(
            @RequestPart("file") MultipartFile file,
            @RequestParam("modelId") Integer modelId) {
        log.info("파일 업로드 API 호출: fileName={}, size={}, modelId={}",
                file.getOriginalFilename(), file.getSize(), modelId);

        FileUploadResponse response = messageService.uploadFile(file, modelId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response));
    }

    /**
     * 특정 채팅방의 메시지 목록을 페이지네이션하여 조회합니다.
     */
    @GetMapping("/page/{roomId}")
    public ResponseEntity<ApiResponse<Page<MessageListItemResponse>>> getMessages(
            @PathVariable UUID roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "createdAt,asc") String sort) {
        log.info("메시지 목록 조회 API 호출: roomId={}, page={}, size={}, sort={}", roomId, page, size, sort);

        // 정렬 파라미터 파싱
        String[] sortParams = sort.split(",");
        String sortField = sortParams[0];
        Sort.Direction direction = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("desc")
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));
        Page<MessageListItemResponse> response = messageService.getMessages(roomId, pageable);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * 특정 메시지 1개의 상세 정보를 조회합니다.
     */
    @GetMapping("/{messageId}")
    public ResponseEntity<ApiResponse<MessageResponse>> getMessage(@PathVariable UUID messageId) {
        log.info("메시지 상세 조회 API 호출: messageId={}", messageId);

        MessageResponse response = messageService.getMessage(messageId);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
