package kr.ai_hub.AI_HUB_BE.controller.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.ai_hub.AI_HUB_BE.application.auth.refreshtoken.RefreshTokenService;
import kr.ai_hub.AI_HUB_BE.domain.user.entity.User;
import kr.ai_hub.AI_HUB_BE.global.application.CookieService;
import kr.ai_hub.AI_HUB_BE.global.auth.CustomOauth2User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final RefreshTokenService refreshTokenService;
    private final CookieService cookieService;

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal CustomOauth2User customOauth2User,
            HttpServletRequest request,
            HttpServletResponse response) {
        log.info("로그아웃 API 호출");

        User user = customOauth2User.getUser();

        // 쿠키에서 리프레시 토큰 확인 (없으면 InvalidTokenException 발생 -> 401)
        Cookie refreshTokenCookie = cookieService.findRefreshTokenCookie(request);
        log.debug("사용자 {} 로그아웃 처리 중", user.getUserId());

        // DB에서 사용자의 모든 토큰 폐기 및 삭제
        refreshTokenService.deleteAllByUser(user);

        // 쿠키 삭제
        cookieService.removeTokenCookiesFromResponse(response);

        log.info("사용자 {} 로그아웃 완료", user.getUserId());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
