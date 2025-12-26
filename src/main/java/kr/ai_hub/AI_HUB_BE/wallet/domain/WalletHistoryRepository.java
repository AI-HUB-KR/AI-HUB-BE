package kr.ai_hub.AI_HUB_BE.wallet.domain;

import kr.ai_hub.AI_HUB_BE.user.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WalletHistoryRepository extends JpaRepository<WalletHistory, Long> {

    Optional<WalletHistory> findByTransactionId(String transactionId);

    List<WalletHistory> findByUser(User user);

    List<WalletHistory> findByUserOrderByCreatedAtDesc(User user);

    List<WalletHistory> findByStatus(String status);

    List<WalletHistory> findByUserAndStatus(User user, String status);

    List<WalletHistory> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    Page<WalletHistory> findByUser(User user, Pageable pageable);

    Page<WalletHistory> findByUserAndStatus(User user, String status, Pageable pageable);
}
