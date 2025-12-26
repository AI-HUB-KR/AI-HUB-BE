package kr.ai_hub.AI_HUB_BE.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.ai_hub.AI_HUB_BE.auth.service.RefreshTokenService;
import kr.ai_hub.AI_HUB_BE.user.domain.User;
import kr.ai_hub.AI_HUB_BE.global.common.CookieService;
import kr.ai_hub.AI_HUB_BE.global.security.oauth2.CustomOAuth2User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "인증", description = "카카오 소셜 로그인 및 토큰 관리")
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final RefreshTokenService refreshTokenService;
    private final CookieService cookieService;

    // AccessToken을 활용하여 유저 정보를 가져온 다음, 로그이웃(토큰 폐기 및 쿠키 삭제)을 진행
    @Operation(summary = "로그아웃")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal CustomOAuth2User customOauth2User,
            HttpServletRequest request,
            HttpServletResponse response) {
        log.info("로그아웃 API 호출");

        User user = customOauth2User.getUser();

        log.debug("사용자 {} 로그아웃 처리 중", user.getUserId());

        // DB에서 사용자의 모든 토큰(Refresh/Access) 폐기 및 삭제
        refreshTokenService.deleteAllByUser(user);

        // 쿠키 삭제
        cookieService.removeTokenCookiesFromResponse(response);

        log.info("사용자 {} 로그아웃 완료", user.getUserId());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
