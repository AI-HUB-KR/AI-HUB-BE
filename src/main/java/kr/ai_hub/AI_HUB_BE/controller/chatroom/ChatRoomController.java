package kr.ai_hub.AI_HUB_BE.controller.chatroom;

import jakarta.validation.Valid;
import kr.ai_hub.AI_HUB_BE.application.chatroom.ChatRoomService;
import kr.ai_hub.AI_HUB_BE.application.chatroom.dto.ChatRoomListItemResponse;
import kr.ai_hub.AI_HUB_BE.application.chatroom.dto.ChatRoomResponse;
import kr.ai_hub.AI_HUB_BE.application.chatroom.dto.CreateChatRoomRequest;
import kr.ai_hub.AI_HUB_BE.application.chatroom.dto.UpdateChatRoomRequest;
import kr.ai_hub.AI_HUB_BE.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/chat-rooms")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    /**
     * 새로운 채팅방을 생성합니다.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ChatRoomResponse>> createChatRoom(@Valid @RequestBody CreateChatRoomRequest request) {
        log.info("채팅방 생성 API 호출: title={}", request.title());

        ChatRoomResponse response = chatRoomService.createChatRoom(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response));
    }

    /**
     * 현재 사용자의 채팅방 목록을 페이지네이션하여 조회합니다.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ChatRoomListItemResponse>>> getChatRooms(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        log.info("채팅방 목록 조회 API 호출: page={}, size={}, sort={}", page, size, sort);

        // 정렬 파라미터 파싱
        String[] sortParams = sort.split(",");
        String sortField = sortParams[0];
        Sort.Direction direction = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));
        Page<ChatRoomListItemResponse> response = chatRoomService.getChatRooms(pageable);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * 특정 채팅방의 상세 정보를 조회합니다.
     */
    @GetMapping("/{roomId}")
    public ResponseEntity<ApiResponse<ChatRoomResponse>> getChatRoom(@PathVariable UUID roomId) {
        log.info("채팅방 상세 조회 API 호출: roomId={}", roomId);

        ChatRoomResponse response = chatRoomService.getChatRoom(roomId);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * 채팅방 제목을 수정합니다.
     */
    @PutMapping("/{roomId}")
    public ResponseEntity<ApiResponse<ChatRoomResponse>> updateChatRoom(
            @PathVariable UUID roomId,
            @Valid @RequestBody UpdateChatRoomRequest request) {
        log.info("채팅방 제목 수정 API 호출: roomId={}, title={}", roomId, request.title());

        ChatRoomResponse response = chatRoomService.updateChatRoom(roomId, request);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * 채팅방을 삭제합니다.
     */
    @DeleteMapping("/{roomId}")
    public ResponseEntity<Void> deleteChatRoom(@PathVariable UUID roomId) {
        log.info("채팅방 삭제 API 호출: roomId={}", roomId);

        chatRoomService.deleteChatRoom(roomId);

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
