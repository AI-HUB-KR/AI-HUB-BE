package kr.ai_hub.AI_HUB_BE.domain.user.repository;

import kr.ai_hub.AI_HUB_BE.user.domain.User;
import kr.ai_hub.AI_HUB_BE.user.domain.UserRepository;
import kr.ai_hub.AI_HUB_BE.user.domain.UserRole;
import kr.ai_hub.AI_HUB_BE.wallet.domain.UserWallet;
import kr.ai_hub.AI_HUB_BE.wallet.domain.UserWalletRepository;
import kr.ai_hub.AI_HUB_BE.global.config.TestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestConfig.class)
class UserWalletRepositoryTest {

    @Autowired
    private UserWalletRepository userWalletRepository;

    @Autowired
    private UserRepository userRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .email("test@example.com")
                .username("Test User")
                .role(UserRole.ROLE_USER)
                .kakaoId("123456789")
                .build();
        userRepository.save(user);
        // Wallet is created by @PostPersist in User, but let's verify or create if
        // needed.
        // Actually, User.java has @PostPersist createWallet().
        // So saving user should create wallet.
    }

    @Test
    @DisplayName("사용자로 지갑 조회")
    void findByUser() {
        // given
        // Wallet is automatically created when User is saved due to @PostPersist

        // when
        Optional<UserWallet> foundWallet = userWalletRepository.findByUser(user);

        // then
        assertThat(foundWallet).isPresent();
        assertThat(foundWallet.get().getUser()).isEqualTo(user);
        assertThat(foundWallet.get().getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("사용자 ID로 지갑 조회")
    void findByUserUserId() {
        // given
        // Wallet is automatically created

        // when
        Optional<UserWallet> foundWallet = userWalletRepository.findByUserUserId(user.getUserId());

        // then
        assertThat(foundWallet).isPresent();
        assertThat(foundWallet.get().getUser()).isEqualTo(user);
    }
}
