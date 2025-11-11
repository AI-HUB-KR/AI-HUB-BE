package kr.ai_hub.AI_HUB_BE.application.message.dto;

import kr.ai_hub.AI_HUB_BE.domain.message.entity.Message;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;

/**
 * 메시지 목록 항목 응답 DTO
 */
@Builder
public record MessageListItemResponse(
        String messageId,
        String role,
        String content,
        BigDecimal tokenCount,
        BigDecimal coinCount,
        Integer modelId,
        Instant createdAt
) {
    public static MessageListItemResponse from(Message message) {
        return MessageListItemResponse.builder()
                .messageId(message.getMessageId().toString())
                .role(message.getRole())
                .content(message.getContent())
                .tokenCount(message.getTokenCount())
                .coinCount(message.getCoinCount())
                .modelId(message.getAiModel() != null ? message.getAiModel().getModelId() : null)
                .createdAt(message.getCreatedAt().toInstant(ZoneOffset.UTC))
                .build();
    }
}
