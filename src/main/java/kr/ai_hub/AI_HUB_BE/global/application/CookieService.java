package kr.ai_hub.AI_HUB_BE.global.application;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.ai_hub.AI_HUB_BE.domain.user.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.WebUtils;

import java.time.Duration;

@Slf4j
@Service
public class CookieService {

    @Value("${jwt.expiration.access}")
    private long accessValidityInSeconds;

    @Value("${jwt.expiration.refresh}")
    private long refreshValidityInSeconds;

    // 토큰 받아서 쿠키 만들어 응답에 추가
    public void addTokenCookiesToResponse(HttpServletResponse response, User user, String refreshToken, String accessToken) {
        ResponseCookie accessTokenCookie = ResponseCookie.from("accessToken", accessToken)
                .httpOnly(true)
                .path("/")
                .maxAge(Duration.ofSeconds(accessValidityInSeconds))
                .sameSite("Strict")
                .build();

        ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .path("/api/token/refresh") // Refresh 엔드포인트에서만 사용 가능
                .maxAge(Duration.ofSeconds(refreshValidityInSeconds))
                .sameSite("Strict")
                .build();

        // [쿠키] 응답에 담기
        response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());
    }


    // 쿠키에서 Refresh토큰 찾아서 반환
    public Cookie findRefreshTokenCookie(HttpServletRequest request) {
        Cookie refreshTokenCookie = WebUtils.getCookie(request, "refreshToken");
        if (refreshTokenCookie == null || !StringUtils.hasText(refreshTokenCookie.getValue())) {
            log.warn("리프레시 토큰 쿠키가 없음");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token cookie is missing");
        }

        return refreshTokenCookie;
    }
}
