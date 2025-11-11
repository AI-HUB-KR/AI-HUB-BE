package kr.ai_hub.AI_HUB_BE.application.message;

import kr.ai_hub.AI_HUB_BE.application.message.dto.MessageListItemResponse;
import kr.ai_hub.AI_HUB_BE.application.message.dto.MessageResponse;
import kr.ai_hub.AI_HUB_BE.domain.chatroom.entity.ChatRoom;
import kr.ai_hub.AI_HUB_BE.domain.chatroom.repository.ChatRoomRepository;
import kr.ai_hub.AI_HUB_BE.domain.message.entity.Message;
import kr.ai_hub.AI_HUB_BE.domain.message.repository.MessageRepository;
import kr.ai_hub.AI_HUB_BE.domain.user.repository.UserRepository;
import kr.ai_hub.AI_HUB_BE.global.error.exception.ForbiddenException;
import kr.ai_hub.AI_HUB_BE.global.error.exception.MessageNotFoundException;
import kr.ai_hub.AI_HUB_BE.global.error.exception.RoomNotFoundException;
import kr.ai_hub.AI_HUB_BE.global.error.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MessageService {

    private final MessageRepository messageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;

    /**
     * 특정 채팅방의 메시지 목록을 페이지네이션하여 조회합니다.
     */
    public Page<MessageListItemResponse> getMessages(UUID roomId, Pageable pageable) {
        Integer userId = getCurrentUserId();
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
        Integer userId = getCurrentUserId();
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
     * SecurityContext에서 현재 인증된 사용자의 ID를 가져옵니다.
     */
    private Integer getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UserNotFoundException("인증되지 않은 사용자입니다");
        }

        try {
            return Integer.parseInt(authentication.getName());
        } catch (NumberFormatException e) {
            log.error("유효하지 않은 사용자 ID 형식: {}", authentication.getName());
            throw new UserNotFoundException("유효하지 않은 사용자 ID입니다");
        }
    }
}
