package kr.ai_hub.AI_HUB_BE.domain.payment;

import kr.ai_hub.AI_HUB_BE.domain.chat.ChatRoom;
import kr.ai_hub.AI_HUB_BE.domain.chat.ChatRoomRepository;
import kr.ai_hub.AI_HUB_BE.domain.user.User;
import kr.ai_hub.AI_HUB_BE.domain.user.UserRepository;
import kr.ai_hub.AI_HUB_BE.domain.user.UserRole;
import kr.ai_hub.AI_HUB_BE.global.config.JpaAuditingConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class CoinTransactionRepositoryTest {

        @Autowired
        private TestEntityManager entityManager;

        @Autowired
        private CoinTransactionRepository coinTransactionRepository;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private ChatRoomRepository chatRoomRepository;

        @Test
        @DisplayName("사용자별 코인 거래 내역 조회")
        void findByUser() {
                // given
                User user = userRepository.save(User.builder()
                                .email("test@example.com")
                                .username("Test User")
                                .role(UserRole.ROLE_USER)
                                .build());

                CoinTransaction tx1 = CoinTransaction.builder()
                                .user(user)
                                .coinUsage(BigDecimal.valueOf(10))
                                .balanceAfter(BigDecimal.valueOf(10))
                                .transactionType("CHARGE")
                                .description("Test Transaction 1")
                                .build();
                coinTransactionRepository.save(tx1);

                CoinTransaction tx2 = CoinTransaction.builder()
                                .user(user)
                                .coinUsage(BigDecimal.valueOf(20))
                                .balanceAfter(BigDecimal.valueOf(30))
                                .transactionType("USAGE")
                                .description("Test Transaction 2")
                                .build();
                coinTransactionRepository.save(tx2);

                // when
                List<CoinTransaction> result = coinTransactionRepository.findByUser(user);

                // then
                assertThat(result).hasSize(2);
                assertThat(result).extracting("transactionId").isNotNull();
        }

        @Test
        @DisplayName("채팅방별 코인 거래 내역 조회")
        void findByChatRoom() {
                // given
                User user = entityManager.persistFlushFind(User.builder()
                                .email("test2@example.com")
                                .username("Test User 2")
                                .role(UserRole.ROLE_USER)
                                .build());

                ChatRoom chatRoom = entityManager.persistFlushFind(ChatRoom.builder()
                                .user(user)
                                .title("Test Room")
                                .build());

                CoinTransaction tx1 = CoinTransaction.builder()
                                .user(user)
                                .chatRoom(chatRoom)
                                .coinUsage(BigDecimal.valueOf(10))
                                .balanceAfter(BigDecimal.valueOf(10))
                                .transactionType("USAGE")
                                .description("Test Transaction 1")
                                .build();
                coinTransactionRepository.save(tx1);

                // when
                List<CoinTransaction> result = coinTransactionRepository.findByChatRoom(chatRoom);

                // then
                assertThat(result).hasSize(1);
                assertThat(result.get(0).getChatRoom().getRoomId()).isEqualTo(chatRoom.getRoomId());
        }

        @Test
        @DisplayName("거래 유형별 조회")
        void findByTransactionType() {
                // given
                User user = userRepository.save(User.builder()
                                .email("test3@example.com")
                                .username("Test User 3")
                                .role(UserRole.ROLE_USER)
                                .build());

                CoinTransaction charge = CoinTransaction.builder()
                                .user(user)
                                .transactionType("CHARGE")
                                .coinUsage(BigDecimal.valueOf(100))
                                .balanceAfter(BigDecimal.valueOf(100))
                                .build();
                CoinTransaction usage = CoinTransaction.builder()
                                .user(user)
                                .transactionType("USAGE")
                                .coinUsage(BigDecimal.valueOf(-50))
                                .balanceAfter(BigDecimal.valueOf(50))
                                .build();
                coinTransactionRepository.saveAll(List.of(charge, usage));

                // when
                List<CoinTransaction> result = coinTransactionRepository.findByTransactionType("CHARGE");

                // then
                assertThat(result).hasSize(1);
                assertThat(result.get(0).getTransactionType()).isEqualTo("CHARGE");
        }

        @Test
        @DisplayName("기간별 조회")
        void findByCreatedAtBetween() {
                // given
                User user = userRepository.save(User.builder()
                                .email("test4@example.com")
                                .username("Test User 4")
                                .role(UserRole.ROLE_USER)
                                .build());

                CoinTransaction tx = CoinTransaction.builder()
                                .user(user)
                                .transactionType("CHARGE")
                                .coinUsage(BigDecimal.valueOf(100))
                                .balanceAfter(BigDecimal.valueOf(100))
                                .build();
                coinTransactionRepository.save(tx);

                LocalDateTime now = LocalDateTime.now();
                LocalDateTime yesterday = now.minusDays(1);
                LocalDateTime tomorrow = now.plusDays(1);

                // when
                List<CoinTransaction> result = coinTransactionRepository.findByCreatedAtBetween(yesterday, tomorrow);

                // then
                assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("사용자별 페이징 조회")
        void findByUserPageable() {
                // given
                User user = userRepository.save(User.builder()
                                .email("test5@example.com")
                                .username("Test User 5")
                                .role(UserRole.ROLE_USER)
                                .build());

                for (int i = 0; i < 10; i++) {
                        coinTransactionRepository.save(CoinTransaction.builder()
                                        .user(user)
                                        .transactionType("CHARGE")
                                        .coinUsage(BigDecimal.valueOf(10))
                                        .balanceAfter(BigDecimal.valueOf((i + 1) * 10))
                                        .build());
                }

                Pageable pageable = PageRequest.of(0, 5);

                // when
                Page<CoinTransaction> result = coinTransactionRepository.findByUser(user, pageable);

                // then
                assertThat(result.getContent()).hasSize(5);
                assertThat(result.getTotalElements()).isEqualTo(10);
        }
}
