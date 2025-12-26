package kr.ai_hub.AI_HUB_BE.global.config;

import kr.ai_hub.AI_HUB_BE.auth.service.CustomOAuth2UserService;
import kr.ai_hub.AI_HUB_BE.global.security.jwt.JwtAuthenticationEntryPoint;
import kr.ai_hub.AI_HUB_BE.global.security.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security 설정
 * - JWT 기반 인증
 * - OAuth2 소셜 로그인 (카카오)
 * - CORS 설정
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /**
     * 인증 없이 접근 가능한 공개 엔드포인트
     */
    private static final String[] PUBLIC_ENDPOINTS = {
            "/h2-console/**",           // H2 데이터베이스 콘솔
            "/login",                   // 로그인 페이지
            "/oauth2/**",               // OAuth2 로그인 엔드포인트
            "/ws/info/**",              // WebSocket 정보
            "/api/v1/token/refresh",    // 토큰 갱신 API
            "/v3/api-docs/**",          // OpenAPI 문서
            "/swagger-ui/**",           // Swagger UI
            "/swagger-ui.html",
            "/favicon.ico",
            "/actuator/**"              // Spring Actuator
    };

    private static final String KAKAO_LOGIN_PAGE = "/oauth2/authorization/kakao";
    private static final String CORS_PATH_PATTERN = "/**";
    private static final List<String> EXPOSED_HEADERS = List.of("Authorization");

    private final CustomOAuth2UserService oAuth2UserService;
    private final AuthenticationSuccessHandler oAuth2SuccessHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Value("${deployment.address}")
    private String deploymentAddress;

    @Value("${cors.allowed-origins}")
    private String[] allowedOrigins;

    /**
     * Spring Security 필터 체인 설정
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // CSRF 비활성화 (JWT 사용으로 불필요)
                .csrf(csrf -> csrf.disable())
                // CORS 설정 활성화
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // H2 Console iframe 허용
                .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.disable()))
                // 세션 미사용 (JWT 기반 STATELESS)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 경로별 인증 규칙
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, CORS_PATH_PATTERN).permitAll() // CORS Preflight
                        .anyRequest().authenticated()
                )
                // OAuth2 소셜 로그인 (카카오)
                .oauth2Login(oauth2 -> oauth2
                        .loginPage(KAKAO_LOGIN_PAGE)
                        .userInfoEndpoint(userInfo -> userInfo.userService(oAuth2UserService))
                        .successHandler(oAuth2SuccessHandler)
                )
                // JWT 검증 필터 등록 (UsernamePasswordAuthenticationFilter 이전에 실행)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // 인증 실패 처리 (401 Unauthorized)
                .exceptionHandling(exception -> exception.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                .build();
    }

    /**
     * CORS 설정
     * - 프로필별 허용 출처 설정 (application.yaml에서 관리)
     * - 모든 HTTP 메서드 및 헤더 허용
     * - 쿠키 포함 요청 허용 (credentials: include)
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 설정 파일에서 읽어온 허용 출처 추가
        for (String origin : allowedOrigins) {
            configuration.addAllowedOrigin(origin.trim());
        }

        configuration.addAllowedMethod("*");        // 모든 HTTP 메서드 허용
        configuration.addAllowedHeader("*");        // 모든 헤더 허용
        configuration.setAllowCredentials(true);    // 쿠키 포함 요청 허용
        configuration.setExposedHeaders(EXPOSED_HEADERS); // Authorization 헤더 노출

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration(CORS_PATH_PATTERN, configuration);
        return source;
    }
}
