package kr.ai_hub.AI_HUB_BE.domain.payment;

import jakarta.persistence.*;
import kr.ai_hub.AI_HUB_BE.domain.user.User;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "wallet_history", indexes = {
        @Index(name = "idx_wallet_transaction_id", columnList = "transaction_id"),
        @Index(name = "idx_wallet_user_created", columnList = "user_id, created_at"),
        @Index(name = "idx_wallet_user_status", columnList = "user_id, status"),
        @Index(name = "idx_wallet_status", columnList = "status"),
        @Index(name = "idx_wallet_created_at", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class WalletHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Long historyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_wallet_history_user"))
    private User user;

    @Column(name = "transaction_id", length = 100, nullable = false, unique = true)
    private String transactionId;

    @Column(name = "payment_method", length = 50, nullable = false)
    private String paymentMethod;

    @Column(name = "pay_amount_krw", precision = 20, scale = 2)
    private BigDecimal payAmountKrw;

    @Column(name = "pay_amount_usd", precision = 20, scale = 2)
    private BigDecimal payAmountUsd;

    @Column(name = "paid_coin", precision = 20, scale = 10, nullable = false)
    private BigDecimal paidCoin;

    @Column(name = "promotion_coin", precision = 20, scale = 10)
    @Builder.Default
    private BigDecimal promotionCoin = BigDecimal.ZERO;

    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private String status = "pending";

    @Enumerated(EnumType.STRING)
    @Column(name = "wallet_history_type", length = 20, nullable = false)
    @Builder.Default
    private WalletHistoryType walletHistoryType = WalletHistoryType.PAID;

    @Column(name = "payment_gateway", length = 50)
    private String paymentGateway;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata")
    private Map<String, Object> metadata;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public void complete() {
        this.status = "completed";
        this.completedAt = LocalDateTime.now();
    }

    public void fail(String reason) {
        this.status = "failed";
        if (this.metadata != null) {
            this.metadata.put("failure_reason", reason);
        }
    }
}
