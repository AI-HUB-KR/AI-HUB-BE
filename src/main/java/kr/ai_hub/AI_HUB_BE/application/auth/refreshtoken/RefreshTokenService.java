package kr.ai_hub.AI_HUB_BE.application.auth.refreshtoken;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import kr.ai_hub.AI_HUB_BE.application.auth.accesstoken.AccessTokenService;
import kr.ai_hub.AI_HUB_BE.application.auth.dto.RefreshedTokens;
import kr.ai_hub.AI_HUB_BE.domain.auth.RefreshToken;
import kr.ai_hub.AI_HUB_BE.domain.auth.RefreshTokenRepository;
import kr.ai_hub.AI_HUB_BE.domain.auth.TokenRevokeReason;
import kr.ai_hub.AI_HUB_BE.domain.user.User;
import kr.ai_hub.AI_HUB_BE.domain.user.UserRepository;
import kr.ai_hub.AI_HUB_BE.global.auth.jwt.JwtTokenProvider;
import kr.ai_hub.AI_HUB_BE.application.auth.TokenHashService;
import kr.ai_hub.AI_HUB_BE.global.error.exception.AuthenticationFailedException;
import kr.ai_hub.AI_HUB_BE.global.error.exception.RefreshTokenInvalidException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final AccessTokenService accessTokenService;
    private final TokenHashService tokenHashService;

    @Value("${jwt.expiration.access}")
    private long accessValidityInSeconds;

    @Value("${jwt.expiration.refresh}")
    private long refreshValidityInSeconds;

    // Refresh Token을 검증하고 새로운 Access Token과 Refresh Token을 발급합니다.
    public RefreshedTokens refreshAccessToken(String rawRefreshToken) {
        log.info("토큰 갱신 요청");

        // JWT 파싱 및 검증
        Claims claims;
        try {
            claims = jwtTokenProvider.parseClaims(rawRefreshToken);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("유효하지 않은 리프레시 토큰: {}", e.getMessage());
            throw new RefreshTokenInvalidException("Invalid refresh token");
        }

        // 사용자 조회
        Integer userId = Integer.valueOf(claims.getSubject());
        log.debug("사용자 {} 토큰 갱신 중", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("사용자를 찾을 수 없음 - ID: {}", userId);
                    return new AuthenticationFailedException("User not found");
                });

        // Refresh Token 검증
        RefreshToken storedToken;
        try {
            storedToken = validateRefreshToken(user, rawRefreshToken);
            // 마지막 사용 시간 업데이트
            storedToken.updateLastUsedAt();
            refreshTokenRepository.save(storedToken);
        } catch (RefreshTokenInvalidException e) {
            log.warn("사용자 {} 리프레시 토큰 검증 실패: {}", userId, e.getMessage());
            throw e;
        }

        // 기존 토큰 폐기
        revokeToken(storedToken, TokenRevokeReason.ROTATED);
        log.debug("사용자 {} 이전 리프레시 토큰 폐기 완료", userId);

        // 새로운 토큰 생성
        String newAccessToken = jwtTokenProvider.createAccessToken(user);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(user);
        RefreshToken newRefreshTokenEntity = saveRefreshToken(user, newRefreshToken);
        accessTokenService.issueAccessToken(user, newAccessToken, newRefreshTokenEntity);

        log.info("사용자 {} 토큰 갱신 완료", userId);
        return new RefreshedTokens(newAccessToken, newRefreshToken, accessValidityInSeconds, refreshValidityInSeconds);
    }

    // 사용자에게 발급할 리프레시 토큰을 저장하고 만료 시점을 설정한다.
    public RefreshToken saveRefreshToken(User user, String refreshToken) {
        log.debug("사용자 {} 리프레시 토큰 저장 중", user.getUserId());

        String tokenHash = tokenHashService.hashToken(refreshToken);
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(refreshValidityInSeconds);

        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(expiresAt)
                .isRevoked(false)
                .build();

        RefreshToken saved = refreshTokenRepository.save(refreshTokenEntity);
        log.debug("사용자 {} 리프레시 토큰 저장 완료", user.getUserId());

        return saved;
    }


    // 리프레시 토큰이 해당 사용자 소유이며 유효한지 검증한다.
    public RefreshToken validateRefreshToken(User user, String rawRefreshToken) {
        String tokenHash = tokenHashService.hashToken(rawRefreshToken);
        RefreshToken stored = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new RefreshTokenInvalidException("Refresh token does not exist"));

        if (!stored.getUser().getUserId().equals(user.getUserId())) {
            throw new RefreshTokenInvalidException("Refresh token owner mismatch");
        }

        if (Boolean.TRUE.equals(stored.getIsRevoked())) {
            throw new RefreshTokenInvalidException("Refresh token already revoked");
        }

        if (stored.isExpired()) {
            stored.revoke(TokenRevokeReason.EXPIRED);
            refreshTokenRepository.save(stored);
            throw new RefreshTokenInvalidException("Refresh token expired");
        }

        return stored;
    }

    // 사용자의 모든 Refresh Token 조회
    @Transactional(readOnly = true)
    public java.util.List<RefreshToken> findByUser(User user) {
        return refreshTokenRepository.findByUser(user);
    }

    // 사용자의 유효한 Refresh Token 조회
    @Transactional(readOnly = true)
    public java.util.List<RefreshToken> findValidTokensByUser(User user) {
        return refreshTokenRepository.findByUserAndIsRevokedFalse(user);
    }

    // Token Hash로 Refresh Token 조회
    @Transactional(readOnly = true)
    public java.util.Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return refreshTokenRepository.findByTokenHash(tokenHash);
    }

    // Refresh Token 폐기 및 연관된 Access Token 폐기
    public void revokeToken(RefreshToken refreshToken, TokenRevokeReason reason) {
        refreshToken.revoke(reason);
        refreshTokenRepository.save(refreshToken);
        accessTokenService.revokeByRefreshToken(refreshToken, reason);
    }

    // 사용자의 모든 Refresh Token과 연관된 Access Token을 삭제 - 사용자 로그아웃 시
    public void deleteAllByUser(User user) {
        List<RefreshToken> tokens = refreshTokenRepository.findByUser(user);
        tokens.forEach(token -> revokeToken(token, TokenRevokeReason.USER_LOGOUT));
        accessTokenService.revokeByUser(user, TokenRevokeReason.USER_LOGOUT);
    }

    // 만료된 Refresh Token 삭제
    public void deleteExpiredTokens() {
        refreshTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    }
}
