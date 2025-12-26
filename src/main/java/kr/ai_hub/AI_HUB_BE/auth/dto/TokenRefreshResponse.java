package kr.ai_hub.AI_HUB_BE.auth.dto;

public record TokenRefreshResponse(String accessToken, long expiresIn) {
}
