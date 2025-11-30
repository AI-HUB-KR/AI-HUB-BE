package kr.ai_hub.AI_HUB_BE.controller.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.ai_hub.AI_HUB_BE.application.auth.dto.RefreshedTokens;
import kr.ai_hub.AI_HUB_BE.application.auth.refreshtoken.RefreshTokenService;
import kr.ai_hub.AI_HUB_BE.domain.user.User;
import kr.ai_hub.AI_HUB_BE.global.application.CookieService;
import kr.ai_hub.AI_HUB_BE.global.auth.WithMockCustomUser;
import kr.ai_hub.AI_HUB_BE.global.error.exception.InvalidTokenException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TokenController.class)
class TokenControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RefreshTokenService refreshTokenService;

    @MockitoBean
    private CookieService cookieService;

    // Security Dependencies Mocks
    @MockitoBean
    private kr.ai_hub.AI_HUB_BE.global.auth.jwt.JwtTokenProvider jwtTokenProvider;
    @MockitoBean
    private kr.ai_hub.AI_HUB_BE.application.auth.CustomOAuth2UserService customOAuth2UserService;
    @MockitoBean
    private org.springframework.security.web.authentication.AuthenticationSuccessHandler authenticationSuccessHandler;
    @MockitoBean
    private kr.ai_hub.AI_HUB_BE.global.auth.jwt.JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    @MockitoBean
    private kr.ai_hub.AI_HUB_BE.application.auth.accesstoken.AccessTokenService accessTokenService;
    @MockitoBean
    private kr.ai_hub.AI_HUB_BE.global.auth.SecurityContextHelper securityContextHelper;
    @MockitoBean
    private org.springframework.data.jpa.mapping.JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    @DisplayName("토큰 갱신 성공")
    @WithMockCustomUser
    void refreshToken_Success() throws Exception {
        // given
        String oldRefreshToken = "old-refresh-token";
        Cookie cookie = new Cookie("refreshToken", oldRefreshToken);

        given(cookieService.findRefreshTokenCookie(any(HttpServletRequest.class))).willReturn(cookie);

        RefreshedTokens newTokens = new RefreshedTokens("new-access-token", "new-refresh-token", 3600L, 86400L);

        given(refreshTokenService.refreshAccessToken(oldRefreshToken)).willReturn(newTokens);

        // when & then
        mockMvc.perform(post("/api/v1/token/refresh")
                .cookie(cookie)
                .with(csrf()))
                .andExpect(status().isOk());

        verify(cookieService).addTokenCookiesToResponse(any(HttpServletResponse.class), any(User.class),
                eq("new-refresh-token"), eq("new-access-token"));
    }

    @Test
    @DisplayName("토큰 갱신 실패 - 리프레시 토큰 쿠키 없음")
    @WithMockCustomUser
    void refreshToken_NoCookie() throws Exception {
        // given
        given(cookieService.findRefreshTokenCookie(any(HttpServletRequest.class)))
                .willThrow(new InvalidTokenException("Refresh token cookie is missing"));

        // when & then
        mockMvc.perform(post("/api/v1/token/refresh")
                .with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}
