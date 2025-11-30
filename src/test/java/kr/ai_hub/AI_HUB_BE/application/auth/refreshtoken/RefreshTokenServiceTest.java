package kr.ai_hub.AI_HUB_BE.application.auth.refreshtoken;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import kr.ai_hub.AI_HUB_BE.application.auth.TokenHashService;
import kr.ai_hub.AI_HUB_BE.application.auth.accesstoken.AccessTokenService;
import kr.ai_hub.AI_HUB_BE.application.auth.dto.RefreshedTokens;
import kr.ai_hub.AI_HUB_BE.domain.auth.RefreshToken;
import kr.ai_hub.AI_HUB_BE.domain.auth.RefreshTokenRepository;
import kr.ai_hub.AI_HUB_BE.domain.auth.TokenRevokeReason;
import kr.ai_hub.AI_HUB_BE.domain.user.User;
import kr.ai_hub.AI_HUB_BE.domain.user.UserRepository;
import kr.ai_hub.AI_HUB_BE.domain.user.UserRole;
import kr.ai_hub.AI_HUB_BE.global.auth.jwt.JwtTokenProvider;
import kr.ai_hub.AI_HUB_BE.global.error.exception.AuthenticationFailedException;
import kr.ai_hub.AI_HUB_BE.global.error.exception.RefreshTokenInvalidException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccessTokenService accessTokenService;

    @Mock
    private TokenHashService tokenHashService;

    @Mock
    private Claims claims;

    private User user;
    private RefreshToken refreshToken;
    private String rawRefreshToken = "raw-refresh-token";
    private String tokenHash = "hashed-token";

    @BeforeEach
    void setUp() {
        user = User.builder()
                .userId(1)
                .username("testuser")
                .email("test@example.com")
                .role(UserRole.ROLE_USER)
                .build();

        refreshToken = RefreshToken.builder()
                .tokenId(1)
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .isRevoked(false)
                .build();

        ReflectionTestUtils.setField(refreshTokenService, "refreshValidityInSeconds", 604800L);
        ReflectionTestUtils.setField(refreshTokenService, "accessValidityInSeconds", 3600L);
    }

    @Test
    @DisplayName("토큰 갱신 성공")
    void refreshAccessToken_Success() {
        // given
        given(jwtTokenProvider.parseClaims(rawRefreshToken)).willReturn(claims);
        given(claims.getSubject()).willReturn("1");
        given(userRepository.findById(1)).willReturn(Optional.of(user));
        given(tokenHashService.hashToken(rawRefreshToken)).willReturn(tokenHash);
        given(refreshTokenRepository.findByTokenHash(tokenHash)).willReturn(Optional.of(refreshToken));
        given(jwtTokenProvider.createAccessToken(user)).willReturn("new-access-token");
        given(jwtTokenProvider.createRefreshToken(user)).willReturn("new-refresh-token");
        given(tokenHashService.hashToken("new-refresh-token")).willReturn("new-hashed-token");
        given(refreshTokenRepository.save(any(RefreshToken.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        RefreshedTokens result = refreshTokenService.refreshAccessToken(rawRefreshToken);

        // then
        assertThat(result.getAccessToken()).isEqualTo("new-access-token");
        assertThat(result.getRefreshToken()).isEqualTo("new-refresh-token");
        verify(accessTokenService).issueAccessToken(eq(user), eq("new-access-token"), any(RefreshToken.class));
        verify(refreshTokenRepository, org.mockito.Mockito.times(2)).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("토큰 갱신 실패 - 유효하지 않은 토큰")
    void refreshAccessToken_InvalidToken() {
        // given
        given(jwtTokenProvider.parseClaims(rawRefreshToken)).willThrow(new JwtException("Invalid token"));

        // when & then
        assertThatThrownBy(() -> refreshTokenService.refreshAccessToken(rawRefreshToken))
                .isInstanceOf(RefreshTokenInvalidException.class)
                .hasMessage("Invalid refresh token");
    }

    @Test
    @DisplayName("토큰 갱신 실패 - 사용자 없음")
    void refreshAccessToken_UserNotFound() {
        // given
        given(jwtTokenProvider.parseClaims(rawRefreshToken)).willReturn(claims);
        given(claims.getSubject()).willReturn("1");
        given(userRepository.findById(1)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> refreshTokenService.refreshAccessToken(rawRefreshToken))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("User not found");
    }

    @Test
    @DisplayName("토큰 갱신 실패 - DB에 토큰 없음")
    void refreshAccessToken_TokenNotFoundInDB() {
        // given
        given(jwtTokenProvider.parseClaims(rawRefreshToken)).willReturn(claims);
        given(claims.getSubject()).willReturn("1");
        given(userRepository.findById(1)).willReturn(Optional.of(user));
        given(tokenHashService.hashToken(rawRefreshToken)).willReturn(tokenHash);
        given(refreshTokenRepository.findByTokenHash(tokenHash)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> refreshTokenService.refreshAccessToken(rawRefreshToken))
                .isInstanceOf(RefreshTokenInvalidException.class)
                .hasMessage("Refresh token does not exist");
    }

    @Test
    @DisplayName("토큰 갱신 실패 - 토큰 소유자 불일치")
    void refreshAccessToken_OwnerMismatch() {
        // given
        User otherUser = User.builder().userId(2).build();
        RefreshToken otherToken = RefreshToken.builder()
                .user(otherUser)
                .tokenHash(tokenHash)
                .isRevoked(false)
                .expiresAt(LocalDateTime.now().plusDays(1))
                .build();

        given(jwtTokenProvider.parseClaims(rawRefreshToken)).willReturn(claims);
        given(claims.getSubject()).willReturn("1");
        given(userRepository.findById(1)).willReturn(Optional.of(user));
        given(tokenHashService.hashToken(rawRefreshToken)).willReturn(tokenHash);
        given(refreshTokenRepository.findByTokenHash(tokenHash)).willReturn(Optional.of(otherToken));

        // when & then
        assertThatThrownBy(() -> refreshTokenService.refreshAccessToken(rawRefreshToken))
                .isInstanceOf(RefreshTokenInvalidException.class)
                .hasMessage("Refresh token owner mismatch");
    }

    @Test
    @DisplayName("토큰 갱신 실패 - 이미 폐기된 토큰")
    void refreshAccessToken_RevokedToken() {
        // given
        refreshToken.revoke(TokenRevokeReason.USER_LOGOUT);

        given(jwtTokenProvider.parseClaims(rawRefreshToken)).willReturn(claims);
        given(claims.getSubject()).willReturn("1");
        given(userRepository.findById(1)).willReturn(Optional.of(user));
        given(tokenHashService.hashToken(rawRefreshToken)).willReturn(tokenHash);
        given(refreshTokenRepository.findByTokenHash(tokenHash)).willReturn(Optional.of(refreshToken));

        // when & then
        assertThatThrownBy(() -> refreshTokenService.refreshAccessToken(rawRefreshToken))
                .isInstanceOf(RefreshTokenInvalidException.class)
                .hasMessage("Refresh token already revoked");
    }

    @Test
    @DisplayName("토큰 갱신 실패 - 만료된 토큰")
    void refreshAccessToken_ExpiredToken() {
        // given
        RefreshToken expiredToken = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .isRevoked(false)
                .expiresAt(LocalDateTime.now().minusDays(1))
                .build();

        given(jwtTokenProvider.parseClaims(rawRefreshToken)).willReturn(claims);
        given(claims.getSubject()).willReturn("1");
        given(userRepository.findById(1)).willReturn(Optional.of(user));
        given(tokenHashService.hashToken(rawRefreshToken)).willReturn(tokenHash);
        given(refreshTokenRepository.findByTokenHash(tokenHash)).willReturn(Optional.of(expiredToken));

        // when & then
        assertThatThrownBy(() -> refreshTokenService.refreshAccessToken(rawRefreshToken))
                .isInstanceOf(RefreshTokenInvalidException.class)
                .hasMessage("Refresh token expired");

        verify(refreshTokenRepository).save(expiredToken); // 만료 처리 저장 확인
    }
}
