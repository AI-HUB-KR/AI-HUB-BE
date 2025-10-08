package kr.ai_hub.AI_HUB_BE.domain.accesstoken.repository;

import kr.ai_hub.AI_HUB_BE.domain.accesstoken.entity.AccessToken;
import kr.ai_hub.AI_HUB_BE.domain.refreshtoken.entity.RefreshToken;
import kr.ai_hub.AI_HUB_BE.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccessTokenRepository extends JpaRepository<AccessToken, Integer> {

    Optional<AccessToken> findByTokenHash(String tokenHash);

    List<AccessToken> findByUser(User user);

    List<AccessToken> findByUserAndIsRevokedFalse(User user);

    List<AccessToken> findByRefreshToken(RefreshToken refreshToken);

    void deleteByExpiresAtBefore(LocalDateTime dateTime);

    void deleteByUser(User user);

    void deleteByRefreshToken(RefreshToken refreshToken);
}
