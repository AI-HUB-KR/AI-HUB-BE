package kr.ai_hub.AI_HUB_BE.application.auth.accesstoken;

import kr.ai_hub.AI_HUB_BE.domain.accesstoken.entity.AccessToken;
import kr.ai_hub.AI_HUB_BE.domain.accesstoken.repository.AccessTokenRepository;
import kr.ai_hub.AI_HUB_BE.domain.refreshtoken.entity.RefreshToken;
import kr.ai_hub.AI_HUB_BE.domain.token.TokenRevokeReason;
import kr.ai_hub.AI_HUB_BE.domain.user.entity.User;
import kr.ai_hub.AI_HUB_BE.application.auth.TokenHashService;
import kr.ai_hub.AI_HUB_BE.global.error.exception.InvalidTokenException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AccessTokenService {

    private final AccessTokenRepository accessTokenRepository;
    private final TokenHashService tokenHashService;

    @Value("${jwt.expiration.access}")
    private long accessValidityInSeconds;

    // 발급된 토큰을 저장(등록)한다.
    public AccessToken issueAccessToken(User user, String rawAccessToken, RefreshToken refreshToken) {
        log.debug("사용자 {} 액세스 토큰 저장 준비", user.getUserId());

        String tokenHash = tokenHashService.hashToken(rawAccessToken);
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(accessValidityInSeconds);

        AccessToken accessToken = AccessToken.builder()
                .user(user)
                .refreshToken(refreshToken)
                .tokenHash(tokenHash)
                .expiresAt(expiresAt)
                .build();

        AccessToken saved = accessTokenRepository.save(accessToken);
        log.debug("사용자 {} 액세스 토큰 저장 완료", user.getUserId());
        return saved;
    }

    // 액세스 토큰을 검증하고 문제 없을 경우 사용 처리한다.
    public AccessToken validateAndUseToken(String rawAccessToken) {
        if (!StringUtils.hasText(rawAccessToken)) {
            throw new InvalidTokenException("Access token is missing");
        }

        String tokenHash = tokenHashService.hashToken(rawAccessToken);
        AccessToken stored = accessTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidTokenException("Access token not registered"));

        if (stored.isExpired()) {
            stored.revoke(TokenRevokeReason.EXPIRED);
            accessTokenRepository.save(stored);
            throw new InvalidTokenException("Access token expired");
        }

        if (Boolean.TRUE.equals(stored.getIsRevoked())) {
            throw new InvalidTokenException("Access token revoked");
        }

        stored.markUsed();
        return accessTokenRepository.save(stored);
    }

    // 특정 리프레시 토큰과 연계된 액세스 토큰을 폐기한다.
    public void revokeByRefreshToken(RefreshToken refreshToken, TokenRevokeReason reason) {
        if (refreshToken == null) {
            return;
        }
        List<AccessToken> tokens = accessTokenRepository.findByRefreshToken(refreshToken);
        tokens.forEach(token -> revokeIfNecessary(token, reason));
    }

    // 사용자의 모든 액세스 토큰을 주어진 사유로 폐기한다.
    public void revokeByUser(User user, TokenRevokeReason reason) {
        List<AccessToken> tokens = accessTokenRepository.findByUser(user);
        tokens.forEach(token -> revokeIfNecessary(token, reason));
    }

    // 만료된 액세스 토큰을 데이터베이스에서 정리한다.
    public void deleteExpiredTokens() {
        accessTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    }

    // 이미 폐기되지 않은 토큰만 사유와 함께 폐기한다.
    private void revokeIfNecessary(AccessToken token, TokenRevokeReason reason) {
        if (Boolean.TRUE.equals(token.getIsRevoked())) {
            return;
        }
        token.revoke(reason);
        accessTokenRepository.save(token);
    }
}
