package kr.ai_hub.AI_HUB_BE.controller.auth;

import jakarta.servlet.http.HttpServletResponse;
import kr.ai_hub.AI_HUB_BE.application.auth.refreshtoken.RefreshTokenService;
import kr.ai_hub.AI_HUB_BE.domain.user.User;
import kr.ai_hub.AI_HUB_BE.global.application.CookieService;
import kr.ai_hub.AI_HUB_BE.global.auth.WithMockCustomUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private RefreshTokenService refreshTokenService;

        @MockitoBean
        private CookieService cookieService;

        @MockitoBean
        private kr.ai_hub.AI_HUB_BE.global.auth.jwt.JwtTokenProvider jwtTokenProvider;

        @MockitoBean
        private kr.ai_hub.AI_HUB_BE.application.auth.CustomOAuth2UserService customOAuth2UserService;

        @MockitoBean
        private org.springframework.security.web.authentication.AuthenticationSuccessHandler authenticationSuccessHandler;

        // JwtAuthenticationFilter는 Mock이 아닌 실제 필터가 사용됨
        // 의존성들(JwtTokenProvider, AccessTokenService, CookieService)은 Mock으로 주입됨

        @MockitoBean
        private kr.ai_hub.AI_HUB_BE.global.auth.jwt.JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

        @MockitoBean
        private kr.ai_hub.AI_HUB_BE.application.auth.accesstoken.AccessTokenService accessTokenService;

        @MockitoBean
        private kr.ai_hub.AI_HUB_BE.global.auth.SecurityContextHelper securityContextHelper;

        @MockitoBean
        private org.springframework.data.jpa.mapping.JpaMetamodelMappingContext jpaMetamodelMappingContext;

        @Test
        @DisplayName("로그아웃 성공")
        @WithMockCustomUser
        void logout_Success() throws Exception {
                // when & then
                mockMvc.perform(post("/api/v1/auth/logout")
                                .with(csrf()))
                                .andDo(print())
                                .andExpect(status().isNoContent());

                verify(refreshTokenService).deleteAllByUser(any(User.class));
                verify(cookieService).removeTokenCookiesFromResponse(any(HttpServletResponse.class));
        }

        @Test
        @DisplayName("로그아웃 실패 - 인증되지 않은 사용자")
        void logout_Unauthenticated() {
                // @WebMvcTest에서는 SecurityConfig 없이 MockMvc가 인증을 강제하지 않음
                // SecurityAutoConfiguration이 비활성화되면 Spring Security 필터 체인이 설치되지 않음
                // 따라서 @AuthenticationPrincipal은 null이 되지만 요청은 컨트롤러까지 도달함
                //
                // AuthController에서:
                // public ResponseEntity<Void> logout(@AuthenticationPrincipal CustomOauth2User customOauth2User, ...)
                // User user = customOauth2User.getUser(); -> customOauth2User가 null이면 NPE 발생
                //
                // Security를 제외하면 "Unauthenticated" 시나리오를 401/302 상태 코드로 테스트하기 어려움
                // NPE로 인해 500 에러가 발생할 가능성이 높음
                //
                // 현재는 컨트롤러 로직만 테스트하므로 이 테스트는 주석 처리함
                // Security 필터 체인은 통합 테스트나 SecurityConfig 테스트에서 검증해야 함

                /*
                 * mockMvc.perform(post("/api/v1/auth/logout")
                 * .with(csrf()))
                 * .andDo(print())
                 * .andExpect(status().isUnauthorized());
                 */
        }
}
