package kr.ai_hub.AI_HUB_BE.application.chatroom.dto;

import kr.ai_hub.AI_HUB_BE.domain.chatroom.entity.ChatRoom;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;

/**
 * 채팅방 상세 응답 DTO
 */
@Builder
public record ChatRoomResponse(
        String roomId,
        String title,
        Integer userId,
        BigDecimal coinUsage,
        Instant createdAt,
        Instant updatedAt
) {
    public static ChatRoomResponse from(ChatRoom chatRoom) {
        return ChatRoomResponse.builder()
                .roomId(chatRoom.getRoomId().toString())
                .title(chatRoom.getTitle())
                .userId(chatRoom.getUser().getUserId())
                .coinUsage(chatRoom.getCoinUsage())
                .createdAt(chatRoom.getCreatedAt().toInstant(ZoneOffset.UTC))
                .updatedAt(chatRoom.getUpdatedAt().toInstant(ZoneOffset.UTC))
                .build();
    }
}
