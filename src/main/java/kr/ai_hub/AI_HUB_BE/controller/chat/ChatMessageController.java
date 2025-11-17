package kr.ai_hub.AI_HUB_BE.controller.chat;

import kr.ai_hub.AI_HUB_BE.application.message.MessageService;
import kr.ai_hub.AI_HUB_BE.application.message.dto.MessageListItemResponse;
import kr.ai_hub.AI_HUB_BE.application.message.dto.MessageResponse;
import kr.ai_hub.AI_HUB_BE.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController("/api/v1/messages")
@RequiredArgsConstructor
public class ChatMessageController {

    private final MessageService messageService;

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
