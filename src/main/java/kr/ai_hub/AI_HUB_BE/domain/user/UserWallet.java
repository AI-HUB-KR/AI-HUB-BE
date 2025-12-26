package kr.ai_hub.AI_HUB_BE.domain.user;

import jakarta.persistence.*;
import kr.ai_hub.AI_HUB_BE.global.error.exception.InsufficientBalanceException;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_wallet", indexes = {
    @Index(name = "idx_user_wallet_user", columnList = "user_id")
})
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

    @Column(name = "paid_balance", precision = 20, scale = 10, nullable = false)
    @Builder.Default
    private BigDecimal paidBalance = BigDecimal.ZERO;

    @Column(name = "promotion_balance", precision = 20, scale = 10, nullable = false)
    @Builder.Default
    private BigDecimal promotionBalance = BigDecimal.ZERO;

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
     * 유상 코인 잔액을 증가시킵니다 (결제를 통한 코인 구매 시 사용).
     *
     * @param amount 증가할 금액
     * @throws IllegalArgumentException 금액(amount)이 null이거나 0 이하인 경우
     */
    public void addPaidBalance(BigDecimal amount) {
        validatePositiveAmount(amount);

        this.paidBalance = this.paidBalance.add(amount);
        this.balance = this.paidBalance.add(this.promotionBalance);
        this.totalPurchased = this.totalPurchased.add(amount);
        this.lastTransactionAt = LocalDateTime.now();
    }

    /**
     * 프로모션 코인 잔액을 증가시킵니다 (관리자 지급 시 사용).
     *
     * @param amount 증가할 금액
     * @throws IllegalArgumentException 금액(amount)이 null이거나 0 이하인 경우
     */
    public void addPromotionBalance(BigDecimal amount) {
        validatePositiveAmount(amount);

        this.promotionBalance = this.promotionBalance.add(amount);
        this.balance = this.paidBalance.add(this.promotionBalance);
        this.lastTransactionAt = LocalDateTime.now();
    }

    /**
     * 프로모션 코인 잔액을 차감합니다 (관리자 회수 시 사용).
     *
     * @param amount 차감할 금액
     * @throws IllegalArgumentException 금액이 null이거나 0 이하인 경우
     * @throws InsufficientBalanceException 프로모션 잔액이 부족한 경우
     */
    public void deductPromotionBalance(BigDecimal amount) {
        validatePositiveAmount(amount);

        if (this.promotionBalance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException("프로모션 잔액이 부족합니다");
        }

        this.promotionBalance = this.promotionBalance.subtract(amount);
        this.balance = this.paidBalance.add(this.promotionBalance);
        this.lastTransactionAt = LocalDateTime.now();
    }

    /**
     * 코인을 사용합니다 (프로모션 코인 선차감 후 유상 코인 차감).
     *
     * @param amount 사용할 금액
     * @throws IllegalArgumentException 금액이 null이거나 0 이하인 경우
     */
    public void deductBalance(BigDecimal amount) {
        validatePositiveAmount(amount);

        BigDecimal remaining = amount;

        // 1. 프로모션 코인부터 차감
        if (this.promotionBalance.compareTo(BigDecimal.ZERO) > 0) {
            if (this.promotionBalance.compareTo(remaining) >= 0) {
                // 프로모션 코인만으로 충분한 경우
                this.promotionBalance = this.promotionBalance.subtract(remaining);
                remaining = BigDecimal.ZERO;
            } else {
                // 프로모션 코인이 부족한 경우, 전부 사용하고 나머지는 유상 코인에서 차감
                remaining = remaining.subtract(this.promotionBalance);
                this.promotionBalance = BigDecimal.ZERO;
            }
        }

        // 2. 남은 금액이 있으면 유상 코인에서 차감
        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            this.paidBalance = this.paidBalance.subtract(remaining);
        }

        // 3. 총액 업데이트
        this.balance = this.paidBalance.add(this.promotionBalance);
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
