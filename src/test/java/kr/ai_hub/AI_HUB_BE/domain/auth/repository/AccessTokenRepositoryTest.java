package kr.ai_hub.AI_HUB_BE.domain.auth.repository;

import kr.ai_hub.AI_HUB_BE.domain.auth.AccessToken;
import kr.ai_hub.AI_HUB_BE.domain.auth.AccessTokenRepository;
import kr.ai_hub.AI_HUB_BE.domain.auth.RefreshToken;
import kr.ai_hub.AI_HUB_BE.domain.auth.RefreshTokenRepository;
import kr.ai_hub.AI_HUB_BE.domain.user.User;
import kr.ai_hub.AI_HUB_BE.domain.user.UserRepository;
import kr.ai_hub.AI_HUB_BE.domain.user.UserRole;
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
class AccessTokenRepositoryTest {

    @Autowired
    private AccessTokenRepository accessTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    private User user;
    private RefreshToken refreshToken;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .email("test@example.com")
                .username("Test User")
                .role(UserRole.ROLE_USER)
                .kakaoId("social-id")
                .build();
        userRepository.save(user);

        refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash("refresh-token")
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
        refreshTokenRepository.save(refreshToken);
    }

    @Test
    @DisplayName("토큰 해시로 액세스 토큰 조회")
    void findByTokenHash() {
        // given
        AccessToken accessToken = AccessToken.builder()
                .user(user)
                .refreshToken(refreshToken)
                .tokenHash("hashed-token")
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();
        accessTokenRepository.save(accessToken);

        // when
        Optional<AccessToken> found = accessTokenRepository.findByTokenHash("hashed-token");

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getTokenHash()).isEqualTo("hashed-token");
    }

    @Test
    @DisplayName("사용자로 액세스 토큰 목록 조회")
    void findByUser() {
        // given
        AccessToken accessToken1 = AccessToken.builder()
                .user(user)
                .refreshToken(refreshToken)
                .tokenHash("hash1")
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();
        AccessToken accessToken2 = AccessToken.builder()
                .user(user)
                .refreshToken(refreshToken)
                .tokenHash("hash2")
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();
        accessTokenRepository.saveAll(List.of(accessToken1, accessToken2));

        // when
        List<AccessToken> tokens = accessTokenRepository.findByUser(user);

        // then
        assertThat(tokens).hasSize(2);
    }

    @Test
    @DisplayName("사용자의 폐기되지 않은 액세스 토큰 조회")
    void findByUserAndIsRevokedFalse() {
        // given
        AccessToken validToken = AccessToken.builder()
                .user(user)
                .refreshToken(refreshToken)
                .tokenHash("valid")
                .expiresAt(LocalDateTime.now().plusHours(1))
                .isRevoked(false)
                .build();

        AccessToken revokedToken = AccessToken.builder()
                .user(user)
                .refreshToken(refreshToken)
                .tokenHash("revoked")
                .expiresAt(LocalDateTime.now().plusHours(1))
                .isRevoked(true)
                .build();

        accessTokenRepository.saveAll(List.of(validToken, revokedToken));

        // when
        List<AccessToken> tokens = accessTokenRepository.findByUserAndIsRevokedFalse(user);

        // then
        assertThat(tokens).hasSize(1);
        assertThat(tokens.get(0).getTokenHash()).isEqualTo("valid");
    }

    @Test
    @DisplayName("리프레시 토큰으로 액세스 토큰 조회")
    void findByRefreshToken() {
        // given
        AccessToken accessToken = AccessToken.builder()
                .user(user)
                .refreshToken(refreshToken)
                .tokenHash("hash")
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();
        accessTokenRepository.save(accessToken);

        // when
        List<AccessToken> tokens = accessTokenRepository.findByRefreshToken(refreshToken);

        // then
        assertThat(tokens).hasSize(1);
        assertThat(tokens.get(0).getRefreshToken()).isEqualTo(refreshToken);
    }

    @Test
    @DisplayName("만료된 토큰 삭제")
    void deleteByExpiresAtBefore() {
        // given
        AccessToken expiredToken = AccessToken.builder()
                .user(user)
                .refreshToken(refreshToken)
                .tokenHash("expired")
                .expiresAt(LocalDateTime.now().minusHours(1))
                .build();

        AccessToken validToken = AccessToken.builder()
                .user(user)
                .refreshToken(refreshToken)
                .tokenHash("valid")
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();

        accessTokenRepository.saveAll(List.of(expiredToken, validToken));

        // when
        accessTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());

        // then
        List<AccessToken> tokens = accessTokenRepository.findAll();
        assertThat(tokens).hasSize(1);
        assertThat(tokens.get(0).getTokenHash()).isEqualTo("valid");
    }
}
