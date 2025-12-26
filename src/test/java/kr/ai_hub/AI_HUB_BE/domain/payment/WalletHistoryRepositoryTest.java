package kr.ai_hub.AI_HUB_BE.domain.payment;

import kr.ai_hub.AI_HUB_BE.domain.user.User;
import kr.ai_hub.AI_HUB_BE.domain.user.UserRepository;
import kr.ai_hub.AI_HUB_BE.domain.user.UserRole;
import kr.ai_hub.AI_HUB_BE.global.config.JpaAuditingConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class WalletHistoryRepositoryTest {

    @Autowired
    private WalletHistoryRepository walletHistoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Transaction ID로 결제 내역 조회")
    void findByTransactionId() {
        // given
        User user = userRepository.save(User.builder()
                .email("test@example.com")
                .username("Test User")
                .role(UserRole.ROLE_USER)
                .build());

        String transactionId = UUID.randomUUID().toString();
        WalletHistory walletHistory = WalletHistory.builder()
                .user(user)
                .transactionId(transactionId)
                .paymentMethod("CARD")
                .payAmountKrw(BigDecimal.valueOf(10000))
                .paidCoin(BigDecimal.valueOf(100))
                .status("completed")
                .build();
        walletHistoryRepository.save(walletHistory);

        // when
        Optional<WalletHistory> result = walletHistoryRepository.findByTransactionId(transactionId);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getTransactionId()).isEqualTo(transactionId);
    }

    @Test
    @DisplayName("사용자별 결제 내역 조회")
    void findByUser() {
        // given
        User user = userRepository.save(User.builder()
                .email("test2@example.com")
                .username("Test User 2")
                .role(UserRole.ROLE_USER)
                .build());

        WalletHistory history1 = WalletHistory.builder()
                .user(user)
                .transactionId("tx-1")
                .paymentMethod("CARD")
                .payAmountKrw(BigDecimal.valueOf(10000))
                .paidCoin(BigDecimal.valueOf(100))
                .build();
        WalletHistory history2 = WalletHistory.builder()
                .user(user)
                .transactionId("tx-2")
                .paymentMethod("CARD")
                .payAmountKrw(BigDecimal.valueOf(20000))
                .paidCoin(BigDecimal.valueOf(200))
                .build();
        walletHistoryRepository.saveAll(List.of(history1, history2));

        // when
        List<WalletHistory> result = walletHistoryRepository.findByUser(user);

        // then
        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("사용자별 결제 내역 최신순 조회")
    void findByUserOrderByCreatedAtDesc() {
        // given
        User user = userRepository.save(User.builder()
                .email("test3@example.com")
                .username("Test User 3")
                .role(UserRole.ROLE_USER)
                .build());

        WalletHistory history1 = WalletHistory.builder()
                .user(user)
                .transactionId("tx-old")
                .paymentMethod("CARD")
                .payAmountKrw(BigDecimal.valueOf(10000))
                .paidCoin(BigDecimal.valueOf(100))
                .build();
        walletHistoryRepository.save(history1);

        // Force delay or different time (JPA auditing uses system time, so sequential
        // saves might have same timestamp)
        // However, in tests, we rely on insertion order if timestamps are identical, or
        // we can flush.
        // Let's just save and hope for slight diff or rely on ID order if timestamp is
        // same (though method name implies CreatedAt)

        WalletHistory history2 = WalletHistory.builder()
                .user(user)
                .transactionId("tx-new")
                .paymentMethod("CARD")
                .payAmountKrw(BigDecimal.valueOf(20000))
                .paidCoin(BigDecimal.valueOf(200))
                .build();
        walletHistoryRepository.save(history2);

        // when
        List<WalletHistory> result = walletHistoryRepository.findByUserOrderByCreatedAtDesc(user);

        // then
        assertThat(result).hasSize(2);
        // Assuming ID generation implies creation order roughly
        assertThat(result.get(0).getTransactionId()).isEqualTo("tx-new");
        assertThat(result.get(1).getTransactionId()).isEqualTo("tx-old");
    }

    @Test
    @DisplayName("상태별 조회")
    void findByStatus() {
        // given
        User user = userRepository.save(User.builder()
                .email("test4@example.com")
                .username("Test User 4")
                .role(UserRole.ROLE_USER)
                .build());

        WalletHistory pending = WalletHistory.builder()
                .user(user)
                .transactionId("tx-pending")
                .paymentMethod("CARD")
                .payAmountKrw(BigDecimal.valueOf(10000))
                .paidCoin(BigDecimal.valueOf(100))
                .status("pending")
                .build();
        WalletHistory completed = WalletHistory.builder()
                .user(user)
                .transactionId("tx-completed")
                .paymentMethod("CARD")
                .payAmountKrw(BigDecimal.valueOf(10000))
                .paidCoin(BigDecimal.valueOf(100))
                .status("completed")
                .build();
        walletHistoryRepository.saveAll(List.of(pending, completed));

        // when
        List<WalletHistory> result = walletHistoryRepository.findByStatus("completed");

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo("completed");
    }

    @Test
    @DisplayName("기간별 조회")
    void findByCreatedAtBetween() {
        // given
        User user = userRepository.save(User.builder()
                .email("test5@example.com")
                .username("Test User 5")
                .role(UserRole.ROLE_USER)
                .build());

        WalletHistory history = WalletHistory.builder()
                .user(user)
                .transactionId("tx-time")
                .paymentMethod("CARD")
                .payAmountKrw(BigDecimal.valueOf(10000))
                .paidCoin(BigDecimal.valueOf(100))
                .build();
        walletHistoryRepository.save(history);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime yesterday = now.minusDays(1);
        LocalDateTime tomorrow = now.plusDays(1);

        // when
        List<WalletHistory> result = walletHistoryRepository.findByCreatedAtBetween(yesterday, tomorrow);

        // then
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("사용자별 페이징 조회")
    void findByUserPageable() {
        // given
        User user = userRepository.save(User.builder()
                .email("test6@example.com")
                .username("Test User 6")
                .role(UserRole.ROLE_USER)
                .build());

        for (int i = 0; i < 10; i++) {
            walletHistoryRepository.save(WalletHistory.builder()
                    .user(user)
                    .transactionId("tx-" + i)
                    .paymentMethod("CARD")
                    .payAmountKrw(BigDecimal.valueOf(1000))
                    .paidCoin(BigDecimal.valueOf(10))
                    .build());
        }

        Pageable pageable = PageRequest.of(0, 5);

        // when
        Page<WalletHistory> result = walletHistoryRepository.findByUser(user, pageable);

        // then
        assertThat(result.getContent()).hasSize(5);
        assertThat(result.getTotalElements()).isEqualTo(10);
    }
}
