package kr.ai_hub.AI_HUB_BE.domain.auth.repository;

import kr.ai_hub.AI_HUB_BE.auth.domain.RefreshToken;
import kr.ai_hub.AI_HUB_BE.auth.domain.RefreshTokenRepository;
import kr.ai_hub.AI_HUB_BE.auth.domain.TokenRevokeReason;
import kr.ai_hub.AI_HUB_BE.user.domain.User;
import kr.ai_hub.AI_HUB_BE.user.domain.UserRole;
import kr.ai_hub.AI_HUB_BE.user.domain.UserRepository;
import kr.ai_hub.AI_HUB_BE.global.config.TestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestConfig.class)
class RefreshTokenRepositoryTest {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .username("tokenuser")
                .email("token@example.com")
                .role(UserRole.ROLE_USER)
                .build();
        userRepository.save(user);
    }

    @Test
    @DisplayName("TokenHash로 리프레시 토큰 조회")
    void findByTokenHash() {
        // given
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash("hash123")
                .expiresAt(LocalDateTime.now().plusDays(7))
                .isRevoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);

        // when
        Optional<RefreshToken> foundToken = refreshTokenRepository.findByTokenHash("hash123");

        // then
        assertThat(foundToken).isPresent();
        assertThat(foundToken.get().getUser().getUserId()).isEqualTo(user.getUserId());
    }

    @Test
    @DisplayName("사용자로 리프레시 토큰 목록 조회")
    void findByUser() {
        // given
        RefreshToken token1 = RefreshToken.builder()
                .user(user)
                .tokenHash("hash1")
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
        RefreshToken token2 = RefreshToken.builder()
                .user(user)
                .tokenHash("hash2")
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
        refreshTokenRepository.saveAll(List.of(token1, token2));

        // when
        List<RefreshToken> tokens = refreshTokenRepository.findByUser(user);

        // then
        assertThat(tokens).hasSize(2);
    }

    @Test
    @DisplayName("사용자의 유효한(revoked되지 않은) 토큰 조회")
    void findByUserAndIsRevokedFalse() {
        // given
        RefreshToken validToken = RefreshToken.builder()
                .user(user)
                .tokenHash("valid")
                .expiresAt(LocalDateTime.now().plusDays(7))
                .isRevoked(false)
                .build();

        RefreshToken revokedToken = RefreshToken.builder()
                .user(user)
                .tokenHash("revoked")
                .expiresAt(LocalDateTime.now().plusDays(7))
                .isRevoked(true)
                .revokedReason(TokenRevokeReason.USER_LOGOUT)
                .build();

        refreshTokenRepository.saveAll(List.of(validToken, revokedToken));

        // when
        List<RefreshToken> validTokens = refreshTokenRepository.findByUserAndIsRevokedFalse(user);

        // then
        assertThat(validTokens).hasSize(1);
        assertThat(validTokens.get(0).getTokenHash()).isEqualTo("valid");
    }

    @Test
    @DisplayName("만료된 토큰 삭제")
    void deleteByExpiresAtBefore() {
        // given
        RefreshToken expiredToken = RefreshToken.builder()
                .user(user)
                .tokenHash("expired")
                .expiresAt(LocalDateTime.now().minusDays(1))
                .build();

        RefreshToken validToken = RefreshToken.builder()
                .user(user)
                .tokenHash("valid")
                .expiresAt(LocalDateTime.now().plusDays(1))
                .build();

        refreshTokenRepository.saveAll(List.of(expiredToken, validToken));

        // when
        refreshTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());

        // then
        List<RefreshToken> remainingTokens = refreshTokenRepository.findAll();
        assertThat(remainingTokens).hasSize(1);
        assertThat(remainingTokens.get(0).getTokenHash()).isEqualTo("valid");
    }

    @Test
    @DisplayName("사용자의 모든 토큰 삭제")
    void deleteByUser() {
        // given
        RefreshToken token1 = RefreshToken.builder()
                .user(user)
                .tokenHash("token1")
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
        refreshTokenRepository.save(token1);

        // when
        refreshTokenRepository.deleteByUser(user);

        // then
        List<RefreshToken> tokens = refreshTokenRepository.findByUser(user);
        assertThat(tokens).isEmpty();
    }
}
