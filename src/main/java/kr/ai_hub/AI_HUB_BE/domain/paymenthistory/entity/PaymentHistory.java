package kr.ai_hub.AI_HUB_BE.domain.paymenthistory.entity;

import jakarta.persistence.*;
import kr.ai_hub.AI_HUB_BE.domain.user.entity.User;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "payment_history")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long paymentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_payment_history_user"))
    private User user;

    @Column(name = "transaction_id", length = 100, nullable = false, unique = true)
    private String transactionId;

    @Column(name = "payment_method", length = 50, nullable = false)
    private String paymentMethod;

    @Column(name = "amount_krw", precision = 20, scale = 2)
    private BigDecimal amountKrw;

    @Column(name = "amount_usd", precision = 20, scale = 2)
    private BigDecimal amountUsd;

    @Column(name = "coin_amount", precision = 20, scale = 10, nullable = false)
    private BigDecimal coinAmount;

    @Column(name = "bonus_coin", precision = 20, scale = 10)
    @Builder.Default
    private BigDecimal bonusCoin = BigDecimal.ZERO;

    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private String status = "pending";

    @Column(name = "payment_gateway", length = 50)
    private String paymentGateway;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
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
