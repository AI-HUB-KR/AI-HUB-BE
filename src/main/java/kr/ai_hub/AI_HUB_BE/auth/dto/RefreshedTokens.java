package kr.ai_hub.AI_HUB_BE.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 토큰 갱신 시 반환되는 새로운 토큰 정보
@Getter
@AllArgsConstructor
public class RefreshedTokens {
    private String accessToken;
    private String refreshToken;
    private long accessTokenExpiresIn;
    private long refreshTokenExpiresIn;
}
