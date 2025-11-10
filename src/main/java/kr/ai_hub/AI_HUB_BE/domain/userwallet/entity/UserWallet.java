package kr.ai_hub.AI_HUB_BE.domain.userwallet.entity;

import jakarta.persistence.*;
import kr.ai_hub.AI_HUB_BE.domain.user.entity.User;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_wallet")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class UserWallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wallet_id")
    private Integer walletId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true, foreignKey = @ForeignKey(name = "fk_user_wallet_user"))
    private User user;

    @Column(name = "balance", precision = 20, scale = 10, nullable = false)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "total_purchased", precision = 20, scale = 10, nullable = false)
    @Builder.Default
    private BigDecimal totalPurchased = BigDecimal.ZERO;

    @Column(name = "total_used", precision = 20, scale = 10, nullable = false)
    @Builder.Default
    private BigDecimal totalUsed = BigDecimal.ZERO;

    @Column(name = "last_transaction_at")
    private LocalDateTime lastTransactionAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void addBalance(BigDecimal amount) {
        this.balance = this.balance.add(amount);
        this.totalPurchased = this.totalPurchased.add(amount);
        this.lastTransactionAt = LocalDateTime.now();
    }

    public void deductBalance(BigDecimal amount) {
        this.balance = this.balance.subtract(amount);
        this.totalUsed = this.totalUsed.add(amount);
        this.lastTransactionAt = LocalDateTime.now();
    }
}
