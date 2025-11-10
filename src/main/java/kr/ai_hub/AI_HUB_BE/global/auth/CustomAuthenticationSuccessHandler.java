package kr.ai_hub.AI_HUB_BE.global.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.ai_hub.AI_HUB_BE.application.auth.accesstoken.AccessTokenService;
import kr.ai_hub.AI_HUB_BE.application.auth.refreshtoken.RefreshTokenService;
import kr.ai_hub.AI_HUB_BE.domain.refreshtoken.entity.RefreshToken;
import kr.ai_hub.AI_HUB_BE.domain.user.entity.User;
import kr.ai_hub.AI_HUB_BE.global.application.CookieService;
import kr.ai_hub.AI_HUB_BE.global.auth.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;


@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final AccessTokenService accessTokenService;
    private final CookieService cookieService;

    @Value("${deployment.frontend.redirect-url}")
    private String frontendRedirectUrl;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException {

        CustomOauth2User customUser = (CustomOauth2User) authentication.getPrincipal();
        User user = customUser.getUser();

        log.info("사용자 {} OAuth2 인증 성공", user.getUserId());

        String accessToken = jwtTokenProvider.createAccessToken(user);
        String refreshToken = jwtTokenProvider.createRefreshToken(user);

        log.debug("사용자 {} 토큰 생성 완료", user.getUserId());

        cookieService.addTokenCookiesToResponse(response, user, refreshToken, accessToken);
        String redirectUrl = frontendRedirectUrl;
        response.sendRedirect(redirectUrl);

        // Token들 DB에 저장
        RefreshToken refreshTokenEntity = refreshTokenService.saveRefreshToken(user, refreshToken);
        accessTokenService.issueAccessToken(user, accessToken, refreshTokenEntity);

        log.info("사용자 {} 인증 처리 완료 - 리다이렉트: {}", user.getUserId(), redirectUrl);
    }
}
