package kr.ai_hub.AI_HUB_BE.domain.chatroom.repository;

import kr.ai_hub.AI_HUB_BE.domain.chatroom.entity.ChatRoom;
import kr.ai_hub.AI_HUB_BE.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, UUID> {

    List<ChatRoom> findByUser(User user);

    List<ChatRoom> findByUserOrderByCreatedAtDesc(User user);
}
