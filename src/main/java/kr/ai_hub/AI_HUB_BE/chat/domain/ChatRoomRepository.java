package kr.ai_hub.AI_HUB_BE.chat.domain;

import kr.ai_hub.AI_HUB_BE.user.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, UUID> {

    List<ChatRoom> findByUser(User user);

    List<ChatRoom> findByUserOrderByCreatedAtDesc(User user);

    Page<ChatRoom> findByUser(User user, Pageable pageable);

    /**
     * 특정 사용자의 전체 채팅방 수를 조회합니다 (최적화된 COUNT 쿼리).
     */
    long countByUser(User user);
}
