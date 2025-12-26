package kr.ai_hub.AI_HUB_BE.global.application;

import jakarta.servlet.http.Cookie;
import kr.ai_hub.AI_HUB_BE.global.common.CookieService;
import kr.ai_hub.AI_HUB_BE.user.domain.User;
import kr.ai_hub.AI_HUB_BE.global.error.exception.InvalidTokenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class CookieServiceTest {

    @InjectMocks
    private CookieService cookieService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(cookieService, "cookieDomain", "localhost");
        ReflectionTestUtils.setField(cookieService, "cookieSecure", false);
        ReflectionTestUtils.setField(cookieService, "cookieSameSite", "Lax");
        ReflectionTestUtils.setField(cookieService, "accessValidityInSeconds", 3600L);
        ReflectionTestUtils.setField(cookieService, "refreshValidityInSeconds", 86400L);
    }

    @Test
    @DisplayName("토큰 쿠키 응답 추가")
    void addTokenCookiesToResponse() {
        // given
        MockHttpServletResponse response = new MockHttpServletResponse();
        User user = User.builder().build();
        String refreshToken = "refresh-token";
        String accessToken = "access-token";

        // when
        cookieService.addTokenCookiesToResponse(response, user, refreshToken, accessToken);

        // then
        assertThat(response.getHeader("Set-Cookie")).contains("accessToken=access-token");
        assertThat(response.getHeaders("Set-Cookie")).anyMatch(s -> s.contains("refreshToken=refresh-token"));
    }

    @Test
    @DisplayName("쿠키에서 액세스 토큰 조회 - 성공")
    void findAccessTokenFromCookie_Success() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("accessToken", "access-token"));

        // when
        String token = cookieService.findAccessTokenFromCookie(request);

        // then
        assertThat(token).isEqualTo("access-token");
    }

    @Test
    @DisplayName("쿠키에서 액세스 토큰 조회 - 실패 (쿠키 없음)")
    void findAccessTokenFromCookie_Missing() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();

        // when
        String token = cookieService.findAccessTokenFromCookie(request);

        // then
        assertThat(token).isNull();
    }

    @Test
    @DisplayName("쿠키에서 리프레시 토큰 조회 - 성공")
    void findRefreshTokenCookie_Success() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("refreshToken", "refresh-token"));

        // when
        Cookie cookie = cookieService.findRefreshTokenCookie(request);

        // then
        assertThat(cookie).isNotNull();
        assertThat(cookie.getValue()).isEqualTo("refresh-token");
    }

    @Test
    @DisplayName("쿠키에서 리프레시 토큰 조회 - 실패 (쿠키 없음)")
    void findRefreshTokenCookie_Missing() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();

        // when & then
        assertThatThrownBy(() -> cookieService.findRefreshTokenCookie(request))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Refresh token cookie is missing");
    }

    @Test
    @DisplayName("로그아웃 시 토큰 쿠키 삭제")
    void removeTokenCookiesFromResponse() {
        // given
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        cookieService.removeTokenCookiesFromResponse(response);

        // then
        assertThat(response.getHeaders("Set-Cookie")).anyMatch(s -> s.contains("accessToken=;"));
        assertThat(response.getHeaders("Set-Cookie")).anyMatch(s -> s.contains("Max-Age=0"));
        assertThat(response.getHeaders("Set-Cookie")).anyMatch(s -> s.contains("refreshToken=;"));
    }
}
