package kr.ai_hub.AI_HUB_BE.chat.dto;

import kr.ai_hub.AI_HUB_BE.chat.domain.Message;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;

/**
 * 메시지 상세 응답 DTO
 */
@Builder
public record MessageResponse(
        String messageId,
        String roomId,
        String role,
        String content,
        String fileUrl,
        BigDecimal tokenCount,
        BigDecimal coinCount,
        Integer modelId,
        Instant createdAt
) {
    public static MessageResponse from(Message message) {
        return MessageResponse.builder()
                .messageId(message.getMessageId().toString())
                .roomId(message.getChatRoom().getRoomId().toString())
                .role(message.getRole().getValue())
                .content(message.getContent())
                .fileUrl(message.getFileUrl())
                .tokenCount(message.getTokenCount())
                .coinCount(message.getCoinCount())
                .modelId(message.getAiModel() != null ? message.getAiModel().getModelId() : null)
                .createdAt(message.getCreatedAt().toInstant(ZoneOffset.UTC))
                .build();
    }
}
