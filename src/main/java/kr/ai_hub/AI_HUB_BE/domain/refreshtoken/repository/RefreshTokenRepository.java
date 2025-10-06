package kr.ai_hub.AI_HUB_BE.domain.refreshtoken.repository;

import kr.ai_hub.AI_HUB_BE.domain.refreshtoken.entity.RefreshToken;
import kr.ai_hub.AI_HUB_BE.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Integer> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findByUser(User user);

    List<RefreshToken> findByUserAndIsRevokedFalse(User user);

    void deleteByExpiresAtBefore(LocalDateTime dateTime);

    void deleteByUser(User user);
}
