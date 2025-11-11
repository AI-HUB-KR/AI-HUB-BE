package kr.ai_hub.AI_HUB_BE.application.chatroom.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 채팅방 목록 항목 응답 DTO
 */
@Builder
public record ChatRoomListItemResponse(
        String roomId,
        String title,
        BigDecimal coinUsage,
        Instant lastMessageAt,
        Instant createdAt
) {}
