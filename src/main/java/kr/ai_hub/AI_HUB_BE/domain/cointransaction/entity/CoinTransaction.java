package kr.ai_hub.AI_HUB_BE.domain.cointransaction.entity;

import jakarta.persistence.*;
import kr.ai_hub.AI_HUB_BE.domain.aimodel.entity.AIModel;
import kr.ai_hub.AI_HUB_BE.domain.chatroom.entity.ChatRoom;
import kr.ai_hub.AI_HUB_BE.domain.message.entity.Message;
import kr.ai_hub.AI_HUB_BE.domain.user.entity.User;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "coin_transaction")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoinTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long transactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_coin_transaction_user"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", foreignKey = @ForeignKey(name = "fk_coin_transaction_chat_room"))
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", foreignKey = @ForeignKey(name = "fk_coin_transaction_message"))
    private Message message;

    @Column(name = "transaction_type", length = 20, nullable = false)
    private String transactionType;

    @Column(name = "amount", precision = 20, scale = 10, nullable = false)
    private BigDecimal amount;

    @Column(name = "balance_after", precision = 20, scale = 10, nullable = false)
    private BigDecimal balanceAfter;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id", foreignKey = @ForeignKey(name = "fk_coin_transaction_ai_model"))
    private AIModel aiModel;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
