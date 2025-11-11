package kr.ai_hub.AI_HUB_BE.domain.userwallet.entity;

import jakarta.persistence.*;
import kr.ai_hub.AI_HUB_BE.domain.user.entity.User;
import kr.ai_hub.AI_HUB_BE.global.error.exception.InsufficientBalanceException;
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

    /**
     * 잔액을 증가시킵니다 (코인 구매 시 사용).
     *
     * @param amount 증가할 금액
     * @throws IllegalArgumentException 금액이 null이거나 0 이하인 경우
     */
    public void addBalance(BigDecimal amount) {
        validatePositiveAmount(amount);

        this.balance = this.balance.add(amount);
        this.totalPurchased = this.totalPurchased.add(amount);
        this.lastTransactionAt = LocalDateTime.now();
    }

    /**
     * 잔액을 차감합니다 (코인 사용 시 사용).
     *
     * @param amount 차감할 금액
     * @throws IllegalArgumentException 금액이 null이거나 0 이하인 경우
     * @throws InsufficientBalanceException 잔액이 부족한 경우
     */
    public void deductBalance(BigDecimal amount) {
        validatePositiveAmount(amount);

        if (this.balance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException(
                String.format("잔액 부족: 현재 잔액 %s, 차감 요청 금액 %s", this.balance, amount)
            );
        }

        this.balance = this.balance.subtract(amount);
        this.totalUsed = this.totalUsed.add(amount);
        this.lastTransactionAt = LocalDateTime.now();
    }

    /**
     * 금액이 양수인지 검증합니다.
     *
     * @param amount 검증할 금액
     * @throws IllegalArgumentException 금액이 null이거나 0 이하인 경우
     */
    private void validatePositiveAmount(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("금액은 null일 수 없습니다");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("금액은 0보다 커야 합니다: " + amount);
        }
    }
}
