package kr.ai_hub.AI_HUB_BE.domain.chat;

import jakarta.persistence.*;
import kr.ai_hub.AI_HUB_BE.domain.user.User;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "chat_room", indexes = {
    @Index(name = "idx_chat_room_user", columnList = "user_id"),
    @Index(name = "idx_chat_room_user_created", columnList = "user_id, created_at")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ChatRoom {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    @Column(name = "room_id", columnDefinition = "UUID")
    private UUID roomId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_chat_room_user"))
    private User user;

    @Column(name = "title", length = 30, nullable = false)
    private String title;

    @Column(name = "coin_usage", precision = 20, scale = 10)
    @Builder.Default
    private BigDecimal coinUsage = BigDecimal.ZERO;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void updateTitle(String title) {
        this.title = title;
    }

    /**
     * 코인 사용량을 증가시킵니다.
     *
     * @param amount 증가시킬 코인 양. 0 이하면 무시됩니다.
     */
    public void addCoinUsage(BigDecimal amount) {
        if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
            this.coinUsage = this.coinUsage.add(amount);
        }
    }
}
