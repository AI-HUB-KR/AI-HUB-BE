package kr.ai_hub.AI_HUB_BE.global.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.ai_hub.AI_HUB_BE.application.auth.accesstoken.AccessTokenService;
import kr.ai_hub.AI_HUB_BE.global.application.CookieService;
import kr.ai_hub.AI_HUB_BE.global.error.exception.InvalidTokenException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final AccessTokenService accessTokenService;
    private final CookieService cookieService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String token = getJwtFromRequest(request);
            if (token == null) {
                filterChain.doFilter(request, response);
                return;
            }

            accessTokenService.validateAndUseToken(token);
            Claims claims = jwtTokenProvider.parseClaims(token);
            Authentication authentication = jwtTokenProvider.getAuthentication(claims);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (InvalidTokenException | JwtException e) {
            log.debug("토큰 검증 실패: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        } catch (IllegalArgumentException e) {
            log.debug("잘못된 토큰 형식: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return false;
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        // 1. Authorization 헤더에서 토큰 시도 (우선순위 높음)
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            log.debug("Authorization 헤더에서 토큰 추출");
            return bearerToken.substring(7);
        }

        // 2. 쿠키에서 accessToken 조회
        String cookieToken = cookieService.findAccessTokenFromCookie(request);
        if (StringUtils.hasText(cookieToken)) {
            log.debug("쿠키에서 accessToken 추출");
            return cookieToken;
        }

        log.debug("토큰을 찾을 수 없음 (헤더 및 쿠키 확인됨)");
        return null;
    }
}
