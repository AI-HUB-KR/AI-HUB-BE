package kr.ai_hub.AI_HUB_BE.application.user.dto;

import kr.ai_hub.AI_HUB_BE.domain.user.User;
import lombok.Builder;

import java.time.Instant;
import java.time.ZoneOffset;

@Builder
public record UserResponse(
        Integer userId,
        String username,
        String email,
        Boolean isActivated,
        Instant createdAt
) {
    public static UserResponse from(User user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .isActivated(user.getIsActivated())
                .createdAt(user.getCreatedAt().toInstant(ZoneOffset.UTC))
                .build();
    }
}
