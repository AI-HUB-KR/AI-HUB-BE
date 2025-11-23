package kr.ai_hub.AI_HUB_BE.domain.chat;

import kr.ai_hub.AI_HUB_BE.domain.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    Page<Message> findByChatRoom(ChatRoom chatRoom, Pageable pageable);

    List<Message> findByChatRoomUser(User user);

    /**
     * 특정 사용자의 전체 메시지 수를 조회합니다 (최적화된 COUNT 쿼리).
     */
    @Query("SELECT COUNT(m) FROM Message m WHERE m.chatRoom.user = :user")
    long countByUser(@Param("user") User user);
}
