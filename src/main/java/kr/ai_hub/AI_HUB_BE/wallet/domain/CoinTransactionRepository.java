package kr.ai_hub.AI_HUB_BE.wallet.domain;

import kr.ai_hub.AI_HUB_BE.chat.domain.ChatRoom;
import kr.ai_hub.AI_HUB_BE.user.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CoinTransactionRepository extends JpaRepository<CoinTransaction, Long> {

    List<CoinTransaction> findByUser(User user);

    List<CoinTransaction> findByUserOrderByCreatedAtDesc(User user);

    List<CoinTransaction> findByChatRoom(ChatRoom chatRoom);

    List<CoinTransaction> findByTransactionType(String transactionType);

    List<CoinTransaction> findByUserAndTransactionType(User user, String transactionType);

    List<CoinTransaction> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    List<CoinTransaction> findByUserAndCreatedAtBetween(User user, LocalDateTime startDate, LocalDateTime endDate);

    Page<CoinTransaction> findByUser(User user, Pageable pageable);

    Page<CoinTransaction> findByUserAndTransactionType(User user, String transactionType, Pageable pageable);

    Page<CoinTransaction> findByUserAndCreatedAtBetween(User user, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    Page<CoinTransaction> findByUserAndTransactionTypeAndCreatedAtBetween(User user, String transactionType, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
}
