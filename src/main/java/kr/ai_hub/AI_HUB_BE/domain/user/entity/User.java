package kr.ai_hub.AI_HUB_BE.domain.user.entity;

import jakarta.persistence.*;
import kr.ai_hub.AI_HUB_BE.domain.userwallet.entity.UserWallet;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "\"user\"", indexes = {
    @Index(name = "idx_user_email", columnList = "email"),
    @Index(name = "idx_user_kakao_id", columnList = "kakao_id"),
    @Index(name = "idx_user_username", columnList = "username"),
    @Index(name = "idx_user_is_deleted", columnList = "is_deleted")
})
@EntityListeners(AuditingEntityListener.class)
@SQLRestriction("is_deleted = false")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "username", length = 50, nullable = false, unique = true)
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 20, nullable = false)
    private UserRole role;

    @Column(name = "email", length = 255, nullable = false, unique = true)
    private String email;

    @Column(name = "kakao_id", length = 255, unique = true)
    private String kakaoId;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Column(name = "is_activated", nullable = false)
    @Builder.Default
    private Boolean isActivated = true;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public void softDelete() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }

    public void update(String username, String email) {
        this.username = username;
        this.email = email;
    }

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private UserWallet wallet;

    @PostPersist  // DB INSERT 직후 실행
      private void createWallet() {
          if (this.wallet == null) {
              this.wallet = UserWallet.builder()
                  .user(this)
                  .balance(BigDecimal.ZERO)
                  .totalPurchased(BigDecimal.ZERO)
                  .totalUsed(BigDecimal.ZERO)
                  .build();
          }
      }
}
