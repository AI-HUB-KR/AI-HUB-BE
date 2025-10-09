package kr.ai_hub.AI_HUB_BE.domain.message.entity;

import jakarta.persistence.*;
import kr.ai_hub.AI_HUB_BE.domain.aimodel.entity.AIModel;
import kr.ai_hub.AI_HUB_BE.domain.chatroom.entity.ChatRoom;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "message")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Message {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    @Column(name = "message_id", columnDefinition = "UUID")
    private UUID messageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false, foreignKey = @ForeignKey(name = "fk_message_chat_room"))
    private ChatRoom chatRoom;

    @Column(name = "role", length = 10, nullable = false)
    private String role;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "file_url", length = 500)
    private String fileUrl;

    @Column(name = "token_count", precision = 20, scale = 10)
    private BigDecimal tokenCount;

    @Column(name = "coin_count", precision = 20, scale = 10)
    private BigDecimal coinCount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id", foreignKey = @ForeignKey(name = "fk_message_ai_model"))
    private AIModel aiModel;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
