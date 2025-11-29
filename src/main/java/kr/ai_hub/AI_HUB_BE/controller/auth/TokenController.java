package kr.ai_hub.AI_HUB_BE.controller.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.ai_hub.AI_HUB_BE.application.auth.dto.RefreshedTokens;
import kr.ai_hub.AI_HUB_BE.application.auth.dto.TokenRefreshResponse;
import kr.ai_hub.AI_HUB_BE.application.auth.refreshtoken.RefreshTokenService;
import kr.ai_hub.AI_HUB_BE.global.application.CookieService;
import kr.ai_hub.AI_HUB_BE.global.auth.CustomOauth2User;
import kr.ai_hub.AI_HUB_BE.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "토큰 관리", description = "Access Token 갱신")
@Slf4j
@RestController
@RequestMapping("/api/v1/token")
@RequiredArgsConstructor
public class TokenController {

    private final RefreshTokenService refreshTokenService;
    private final CookieService cookieService;

    @Operation(summary = "토큰 갱신")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenRefreshResponse>> refreshToken(
            @AuthenticationPrincipal CustomOauth2User customOauth2User, HttpServletRequest request, HttpServletResponse response) {
        log.info("토큰 갱신 API 호출");

        Cookie refreshTokenCookie = cookieService.findRefreshTokenCookie(request);
        String rawRefreshToken = refreshTokenCookie.getValue();

        // 토큰 검증 및 새 토큰 발급
        RefreshedTokens tokens = refreshTokenService.refreshAccessToken(rawRefreshToken);

        cookieService.addTokenCookiesToResponse(response, customOauth2User.getUser(),
                tokens.getRefreshToken(),tokens.getAccessToken());


        log.info("토큰 갱신 성공");
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
