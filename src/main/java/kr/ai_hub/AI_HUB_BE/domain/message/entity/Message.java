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
@Table(name = "message", indexes = {
    @Index(name = "idx_message_room_created", columnList = "room_id, created_at")
})
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

    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 10, nullable = false)
    private MessageRole role;

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

    @Column(name = "response_id", length = 100)
    private String responseId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 토큰 수와 코인 수를 업데이트합니다.
     * 도메인 로직: 메시지의 토큰 및 코인 정보를 설정
     *
     * @param tokenCount 토큰 수
     * @param coinCount 코인 수
     */
    public void updateTokenAndCoin(BigDecimal tokenCount, BigDecimal coinCount) {
        this.tokenCount = tokenCount;
        this.coinCount = coinCount;
    }

    /**
     * AI 응답 ID를 업데이트합니다.
     * USER 메시지에 대응하는 ASSISTANT 메시지의 ID를 참조
     *
     * @param responseId AI 응답 메시지 ID
     */
    public void updateResponseId(String responseId) {
        this.responseId = responseId;
    }
}
