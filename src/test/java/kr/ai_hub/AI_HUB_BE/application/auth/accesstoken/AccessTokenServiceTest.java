package kr.ai_hub.AI_HUB_BE.application.auth.accesstoken;

import kr.ai_hub.AI_HUB_BE.application.auth.TokenHashService;
import kr.ai_hub.AI_HUB_BE.domain.auth.AccessToken;
import kr.ai_hub.AI_HUB_BE.domain.auth.AccessTokenRepository;
import kr.ai_hub.AI_HUB_BE.domain.auth.RefreshToken;
import kr.ai_hub.AI_HUB_BE.domain.auth.TokenRevokeReason;
import kr.ai_hub.AI_HUB_BE.domain.user.User;
import kr.ai_hub.AI_HUB_BE.global.error.exception.InvalidTokenException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AccessTokenServiceTest {

    @InjectMocks
    private AccessTokenService accessTokenService;

    @Mock
    private AccessTokenRepository accessTokenRepository;

    @Mock
    private TokenHashService tokenHashService;

    @Test
    @DisplayName("액세스 토큰 발급 및 저장")
    void issueAccessToken() {
        // given
        User user = User.builder().email("test@example.com").build();
        RefreshToken refreshToken = RefreshToken.builder().build();
        String rawToken = "raw-token";
        String hashedToken = "hashed-token";

        ReflectionTestUtils.setField(accessTokenService, "accessValidityInSeconds", 3600L);

        given(tokenHashService.hashToken(rawToken)).willReturn(hashedToken);
        given(accessTokenRepository.save(any(AccessToken.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        AccessToken result = accessTokenService.issueAccessToken(user, rawToken, refreshToken);

        // then
        assertThat(result.getTokenHash()).isEqualTo(hashedToken);
        assertThat(result.getUser()).isEqualTo(user);
        assertThat(result.getRefreshToken()).isEqualTo(refreshToken);
        assertThat(result.getExpiresAt()).isAfter(LocalDateTime.now());
    }

    @Test
    @DisplayName("토큰 검증 및 사용 - 성공")
    void validateAndUseToken_Success() {
        // given
        String rawToken = "raw-token";
        String hashedToken = "hashed-token";
        AccessToken accessToken = AccessToken.builder()
                .tokenHash(hashedToken)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .isRevoked(false)
                .build();

        given(tokenHashService.hashToken(rawToken)).willReturn(hashedToken);
        given(accessTokenRepository.findByTokenHash(hashedToken)).willReturn(Optional.of(accessToken));
        given(accessTokenRepository.save(any(AccessToken.class))).willReturn(accessToken);

        // when
        AccessToken result = accessTokenService.validateAndUseToken(rawToken);

        // then
        assertThat(result).isEqualTo(accessToken);
        assertThat(result.getLastUsedAt()).isNotNull();
    }

    @Test
    @DisplayName("토큰 검증 실패 - 토큰 없음")
    void validateAndUseToken_Missing() {
        assertThatThrownBy(() -> accessTokenService.validateAndUseToken(null))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Access token is missing");
    }

    @Test
    @DisplayName("토큰 검증 실패 - 등록되지 않은 토큰")
    void validateAndUseToken_NotRegistered() {
        // given
        String rawToken = "raw-token";
        String hashedToken = "hashed-token";

        given(tokenHashService.hashToken(rawToken)).willReturn(hashedToken);
        given(accessTokenRepository.findByTokenHash(hashedToken)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> accessTokenService.validateAndUseToken(rawToken))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Access token not registered");
    }

    @Test
    @DisplayName("토큰 검증 실패 - 만료된 토큰")
    void validateAndUseToken_Expired() {
        // given
        String rawToken = "raw-token";
        String hashedToken = "hashed-token";
        AccessToken accessToken = AccessToken.builder()
                .tokenHash(hashedToken)
                .expiresAt(LocalDateTime.now().minusHours(1))
                .isRevoked(false)
                .build();

        given(tokenHashService.hashToken(rawToken)).willReturn(hashedToken);
        given(accessTokenRepository.findByTokenHash(hashedToken)).willReturn(Optional.of(accessToken));

        // when & then
        assertThatThrownBy(() -> accessTokenService.validateAndUseToken(rawToken))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Access token expired");

        verify(accessTokenRepository).save(accessToken); // Revoke 처리 저장 확인
        assertThat(accessToken.getRevokedReason()).isEqualTo(TokenRevokeReason.EXPIRED);
    }

    @Test
    @DisplayName("토큰 검증 실패 - 폐기된 토큰")
    void validateAndUseToken_Revoked() {
        // given
        String rawToken = "raw-token";
        String hashedToken = "hashed-token";
        AccessToken accessToken = AccessToken.builder()
                .tokenHash(hashedToken)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .isRevoked(true)
                .build();

        given(tokenHashService.hashToken(rawToken)).willReturn(hashedToken);
        given(accessTokenRepository.findByTokenHash(hashedToken)).willReturn(Optional.of(accessToken));

        // when & then
        assertThatThrownBy(() -> accessTokenService.validateAndUseToken(rawToken))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Access token revoked");
    }

    @Test
    @DisplayName("리프레시 토큰으로 액세스 토큰 폐기")
    void revokeByRefreshToken() {
        // given
        RefreshToken refreshToken = RefreshToken.builder().build();
        AccessToken accessToken = AccessToken.builder()
                .refreshToken(refreshToken)
                .isRevoked(false)
                .build();

        given(accessTokenRepository.findByRefreshToken(refreshToken)).willReturn(List.of(accessToken));

        // when
        accessTokenService.revokeByRefreshToken(refreshToken, TokenRevokeReason.USER_LOGOUT);

        // then
        assertThat(accessToken.getIsRevoked()).isTrue();
        assertThat(accessToken.getRevokedReason()).isEqualTo(TokenRevokeReason.USER_LOGOUT);
        verify(accessTokenRepository).save(accessToken);
    }
}
