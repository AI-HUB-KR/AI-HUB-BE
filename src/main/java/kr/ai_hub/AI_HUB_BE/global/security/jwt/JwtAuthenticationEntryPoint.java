package kr.ai_hub.AI_HUB_BE.global.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.ai_hub.AI_HUB_BE.global.common.response.ApiResponse;
import kr.ai_hub.AI_HUB_BE.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        log.warn("인증 실패 - {}: {}", request.getRequestURI(), authException.getMessage());

        // 인증 실패 시 401 Unauthorized 응답 (ApiResponse 형식)
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        ApiResponse<?> apiResponse = ApiResponse.error(ErrorCode.AUTHENTICATION_FAILED);
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}