package kr.ai_hub.AI_HUB_BE.domain.message.repository;

import kr.ai_hub.AI_HUB_BE.domain.chatroom.entity.ChatRoom;
import kr.ai_hub.AI_HUB_BE.domain.message.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findByChatRoom(ChatRoom chatRoom);

    List<Message> findByChatRoomOrderByCreatedAtAsc(ChatRoom chatRoom);

    List<Message> findByChatRoomRoomId(UUID roomId);

    Optional<Message> findTopByChatRoomOrderByCreatedAtDesc(ChatRoom chatRoom);
}
