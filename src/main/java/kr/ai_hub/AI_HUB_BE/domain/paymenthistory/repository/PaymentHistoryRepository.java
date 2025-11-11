package kr.ai_hub.AI_HUB_BE.domain.paymenthistory.repository;

import kr.ai_hub.AI_HUB_BE.domain.paymenthistory.entity.PaymentHistory;
import kr.ai_hub.AI_HUB_BE.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentHistoryRepository extends JpaRepository<PaymentHistory, Long> {

    Optional<PaymentHistory> findByTransactionId(String transactionId);

    List<PaymentHistory> findByUser(User user);

    List<PaymentHistory> findByUserOrderByCreatedAtDesc(User user);

    List<PaymentHistory> findByStatus(String status);

    List<PaymentHistory> findByUserAndStatus(User user, String status);

    List<PaymentHistory> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    Page<PaymentHistory> findByUser(User user, Pageable pageable);

    Page<PaymentHistory> findByUserAndStatus(User user, String status, Pageable pageable);
}
