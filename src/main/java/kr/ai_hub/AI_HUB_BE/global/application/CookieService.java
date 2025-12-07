package kr.ai_hub.AI_HUB_BE.global.application;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.ai_hub.AI_HUB_BE.domain.user.User;
import kr.ai_hub.AI_HUB_BE.global.error.exception.InvalidTokenException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.WebUtils;

import java.time.Duration;

@Slf4j
@Service
public class CookieService {
    // Cookie 발급 주체 (Spring 서버 도메인)
    @Value("${cookie.domain:}")
    private String cookieDomain;

    @Value("${cookie.refresh.path}")
    private String refreshCookiePath;

    @Value("${cookie.secure}")
    private boolean cookieSecure;

    @Value("${cookie.same-site}")
    private String cookieSameSite;

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
                .sameSite(cookieSameSite)
                .secure(cookieSecure)
                .domain(cookieDomain)
                .build();

        ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .path(refreshCookiePath)
                .maxAge(Duration.ofSeconds(refreshValidityInSeconds))
                .sameSite(cookieSameSite)
                .secure(cookieSecure)
                .domain(cookieDomain)
                .build();

        // [쿠키] 응답에 담기
        response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());
    }


    // 쿠키에서 Access토큰 찾아서 반환
    public String findAccessTokenFromCookie(HttpServletRequest request) {
        Cookie accessTokenCookie = WebUtils.getCookie(request, "accessToken");
        if (accessTokenCookie == null || !StringUtils.hasText(accessTokenCookie.getValue())) {
            log.debug("액세스 토큰 쿠키가 없음");
            return null;
        }

        return accessTokenCookie.getValue();
    }

    // 쿠키에서 Refresh토큰 찾아서 반환
    public Cookie findRefreshTokenCookie(HttpServletRequest request) {
        Cookie refreshTokenCookie = WebUtils.getCookie(request, "refreshToken");
        if (refreshTokenCookie == null || !StringUtils.hasText(refreshTokenCookie.getValue())) {
            log.warn("리프레시 토큰 쿠키가 없음");
            throw new InvalidTokenException("Refresh token cookie is missing");
        }

        return refreshTokenCookie;
    }

    // 로그아웃 시 브라우저의 Access/Refresh 토큰 쿠키 삭제(빈 값과 만료 시간 0으로 설정)
    public void removeTokenCookiesFromResponse(HttpServletResponse response) {
        ResponseCookie accessTokenCookie = ResponseCookie.from("accessToken", "")
                .httpOnly(true)
                .path("/")
                .maxAge(0)
                .sameSite("Strict")
                .build();

        ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .path("/api/token/refresh")
                .maxAge(0)
                .sameSite("Strict")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());
    }
}
