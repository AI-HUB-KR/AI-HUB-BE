package kr.ai_hub.AI_HUB_BE.auth.domain;

import jakarta.persistence.*;
import kr.ai_hub.AI_HUB_BE.user.domain.User;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "access_token", indexes = {
    @Index(name = "idx_access_token_hash", columnList = "token_hash"),
    @Index(name = "idx_access_token_user_revoked", columnList = "user_id, is_revoked"),
    @Index(name = "idx_access_token_expires_at", columnList = "expires_at"),
    @Index(name = "idx_access_token_refresh_token", columnList = "refresh_token_id")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class AccessToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "token_id")
    private Integer tokenId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_access_token_user"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "refresh_token_id", foreignKey = @ForeignKey(name = "fk_access_token_refresh_token"))
    private RefreshToken refreshToken;

    @Column(name = "token_hash", length = 64, nullable = false, unique = true)
    private String tokenHash;

    @CreatedDate
    @Column(name = "issued_at", nullable = false, updatable = false)
    private LocalDateTime issuedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "is_revoked", nullable = false)
    @Builder.Default
    private Boolean isRevoked = false;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "revoked_reason", length = 100)
    private TokenRevokeReason revokedReason;

    public void markUsed() {
        this.lastUsedAt = LocalDateTime.now();
    }

    public void revoke(TokenRevokeReason reason) {
        this.isRevoked = true;
        this.revokedAt = LocalDateTime.now();
        this.revokedReason = reason;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !isRevoked && !isExpired();
    }
}
