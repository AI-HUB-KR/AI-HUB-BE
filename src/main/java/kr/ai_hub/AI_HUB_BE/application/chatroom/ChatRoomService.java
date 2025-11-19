package kr.ai_hub.AI_HUB_BE.application.chatroom;

import kr.ai_hub.AI_HUB_BE.application.chatroom.dto.ChatRoomListItemResponse;
import kr.ai_hub.AI_HUB_BE.application.chatroom.dto.ChatRoomResponse;
import kr.ai_hub.AI_HUB_BE.application.chatroom.dto.CreateChatRoomRequest;
import kr.ai_hub.AI_HUB_BE.application.chatroom.dto.UpdateChatRoomRequest;
import kr.ai_hub.AI_HUB_BE.domain.aimodel.entity.AIModel;
import kr.ai_hub.AI_HUB_BE.domain.aimodel.repository.AIModelRepository;
import kr.ai_hub.AI_HUB_BE.domain.chatroom.entity.ChatRoom;
import kr.ai_hub.AI_HUB_BE.domain.chatroom.repository.ChatRoomRepository;
import kr.ai_hub.AI_HUB_BE.domain.message.entity.Message;
import kr.ai_hub.AI_HUB_BE.domain.message.repository.MessageRepository;
import kr.ai_hub.AI_HUB_BE.domain.user.entity.User;
import kr.ai_hub.AI_HUB_BE.domain.user.repository.UserRepository;
import kr.ai_hub.AI_HUB_BE.global.auth.SecurityContextHelper;
import kr.ai_hub.AI_HUB_BE.global.error.exception.ForbiddenException;
import kr.ai_hub.AI_HUB_BE.global.error.exception.ModelNotFoundException;
import kr.ai_hub.AI_HUB_BE.global.error.exception.RoomNotFoundException;
import kr.ai_hub.AI_HUB_BE.global.error.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final AIModelRepository aiModelRepository;
    private final MessageRepository messageRepository;
    private final SecurityContextHelper securityContextHelper;

    /**
     * 새로운 채팅방을 생성합니다.
     */
    @Transactional
    public ChatRoomResponse createChatRoom(CreateChatRoomRequest request) {
        Integer userId = securityContextHelper.getCurrentUserId();
        log.info("사용자 {} 채팅방 생성 요청: {}", userId, request.title());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + userId));

        // AI 모델 검증 (활성 상태인지 확인)
        AIModel aiModel = aiModelRepository.findById(request.modelId())
                .orElseThrow(() -> new ModelNotFoundException("AI 모델을 찾을 수 없습니다: " + request.modelId()));

        if (!aiModel.getIsActive()) {
            log.warn("비활성 모델 사용 시도: modelId={}", request.modelId());
            throw new ModelNotFoundException("해당 AI 모델은 현재 사용할 수 없습니다");
        }

        ChatRoom chatRoom = ChatRoom.builder()
                .user(user)
                .title(request.title())
                .build();

        ChatRoom savedChatRoom = chatRoomRepository.save(chatRoom);
        log.info("채팅방 생성 완료: roomId={}", savedChatRoom.getRoomId());

        return ChatRoomResponse.from(savedChatRoom);
    }

    /**
     * 현재 사용자의 채팅방 목록을 페이지네이션하여 조회합니다.
     */
    public Page<ChatRoomListItemResponse> getChatRooms(Pageable pageable) {
        Integer userId = securityContextHelper.getCurrentUserId();
        log.debug("사용자 {} 채팅방 목록 조회 (page={}, size={})", userId, pageable.getPageNumber(), pageable.getPageSize());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + userId));

        Page<ChatRoom> chatRooms = chatRoomRepository.findByUser(user, pageable);

        return chatRooms.map(chatRoom -> {
            // 각 채팅방의 마지막 메시지 시각 조회
            Message lastMessage = messageRepository.findTopByChatRoomOrderByCreatedAtDesc(chatRoom)
                    .orElse(null);

            return ChatRoomListItemResponse.builder()
                    .roomId(chatRoom.getRoomId().toString())
                    .title(chatRoom.getTitle())
                    .coinUsage(chatRoom.getCoinUsage())
                    .lastMessageAt(lastMessage != null ? lastMessage.getCreatedAt().toInstant(ZoneOffset.UTC) : null)
                    .createdAt(chatRoom.getCreatedAt().toInstant(ZoneOffset.UTC))
                    .build();
        });
    }

    /**
     * 특정 채팅방의 상세 정보를 조회합니다.
     */
    public ChatRoomResponse getChatRoom(UUID roomId) {
        Integer userId = securityContextHelper.getCurrentUserId();
        log.debug("채팅방 {} 상세 조회 요청 by 사용자 {}", roomId, userId);

        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("채팅방을 찾을 수 없습니다: " + roomId));

        // 권한 확인: 채팅방 소유자만 조회 가능
        if (!chatRoom.getUser().getUserId().equals(userId)) {
            log.warn("채팅방 접근 권한 없음: roomId={}, userId={}", roomId, userId);
            throw new ForbiddenException("해당 채팅방에 접근할 권한이 없습니다");
        }

        return ChatRoomResponse.from(chatRoom);
    }

    /**
     * 채팅방 제목을 수정합니다.
     */
    @Transactional
    public ChatRoomResponse updateChatRoom(UUID roomId, UpdateChatRoomRequest request) {
        Integer userId = securityContextHelper.getCurrentUserId();
        log.info("채팅방 {} 제목 수정 요청 by 사용자 {}", roomId, userId);

        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("채팅방을 찾을 수 없습니다: " + roomId));

        // 권한 확인: 채팅방 소유자만 수정 가능
        if (!chatRoom.getUser().getUserId().equals(userId)) {
            log.warn("채팅방 수정 권한 없음: roomId={}, userId={}", roomId, userId);
            throw new ForbiddenException("해당 채팅방을 수정할 권한이 없습니다");
        }

        chatRoom.updateTitle(request.title());
        log.info("채팅방 {} 제목 수정 완료: {}", roomId, request.title());

        return ChatRoomResponse.from(chatRoom);
    }

    /**
     * 채팅방을 삭제합니다.
     */
    @Transactional
    public void deleteChatRoom(UUID roomId) {
        Integer userId = securityContextHelper.getCurrentUserId();
        log.info("채팅방 {} 삭제 요청 by 사용자 {}", roomId, userId);

        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("채팅방을 찾을 수 없습니다: " + roomId));

        // 권한 확인: 채팅방 소유자만 삭제 가능
        if (!chatRoom.getUser().getUserId().equals(userId)) {
            log.warn("채팅방 삭제 권한 없음: roomId={}, userId={}", roomId, userId);
            throw new ForbiddenException("해당 채팅방을 삭제할 권한이 없습니다");
        }

        chatRoomRepository.delete(chatRoom);
        log.info("채팅방 {} 삭제 완료", roomId);
    }
}
