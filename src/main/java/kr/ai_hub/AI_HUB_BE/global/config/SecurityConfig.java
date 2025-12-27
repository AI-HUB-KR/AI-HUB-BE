package kr.ai_hub.AI_HUB_BE.global.config;

import kr.ai_hub.AI_HUB_BE.auth.service.CustomOAuth2UserService;
import kr.ai_hub.AI_HUB_BE.global.security.jwt.JwtAuthenticationEntryPoint;
import kr.ai_hub.AI_HUB_BE.global.security.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
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

import java.util.ArrayList;
import java.util.Arrays;
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
     * 모든 프로필에서 공통으로 허용되는 엔드포인트
     */
    private static final String[] COMMON_PUBLIC_ENDPOINTS = {
            "/login",                   // 로그인 페이지
            "/oauth2/**",               // OAuth2 로그인 엔드포인트
            "/api/v1/token/refresh",    // 토큰 갱신 API
            "/favicon.ico",             // 파비콘
            "/actuator/**"              // Spring Actuator
    };

    /**
     * dev 프로필에서만 추가로 허용되는 엔드포인트
     */
    private static final String[] DEV_ONLY_ENDPOINTS = {
            "/h2-console/**",           // H2 데이터베이스 콘솔
            "/v3/api-docs/**",          // OpenAPI 문서
            "/swagger-ui/**",           // Swagger UI
            "/swagger-ui.html"          // Swagger UI 메인
    };

    private static final String KAKAO_LOGIN_PAGE = "/oauth2/authorization/kakao";
    private static final String CORS_PATH_PATTERN = "/**";
    private static final List<String> EXPOSED_HEADERS = List.of("Authorization");

    private final Environment environment;
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
     * - dev 프로필: H2 콘솔, Swagger UI 허용
     * - prod 프로필: 보안 강화 (개발 도구 차단)
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // 프로필별 허용 엔드포인트 동적 생성
        String[] publicEndpoints = getPublicEndpoints();

        return http
                // CSRF 비활성화 (JWT 사용으로 불필요)
                .csrf(csrf -> csrf.disable())
                // CORS 설정 활성화
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // H2 Console iframe 허용 (dev 프로필에서만 의미 있음)
                .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.disable()))
                // 세션 미사용 (JWT 기반 STATELESS)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 경로별 인증 규칙 (프로필별 동적 적용)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(publicEndpoints).permitAll()
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
     * 프로필별 허용 엔드포인트 동적 생성
     * - dev: 공통 + 개발 도구 (H2, Swagger)
     * - prod: 공통만 허용
     */
    private String[] getPublicEndpoints() {
        List<String> endpoints = new ArrayList<>(Arrays.asList(COMMON_PUBLIC_ENDPOINTS));

        // dev 프로필일 때만 개발 도구 엔드포인트 추가
        if (Arrays.asList(environment.getActiveProfiles()).contains("dev")) {
            endpoints.addAll(Arrays.asList(DEV_ONLY_ENDPOINTS));
        }

        return endpoints.toArray(new String[0]);
    }

    /**
     * CORS 설정
     * - 프로필별 허용 출처 설정 (application.yaml에서 관리)
     * - 프로젝트에서 실제 사용하는 메서드와 헤더만 명시적으로 허용
     * - 쿠키 포함 요청 허용 (credentials: include)
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 설정 파일에서 읽어온 허용 출처 추가
        for (String origin : allowedOrigins) {
            configuration.addAllowedOrigin(origin.trim());
        }

        // 실제 사용하는 HTTP 메서드만 명시적으로 허용
        configuration.setAllowedMethods(List.of(
                "GET",      // 조회
                "POST",     // 생성, 로그인, 메시지 전송
                "PUT",      // 전체 수정
                "PATCH",    // 부분 수정
                "DELETE",   // 삭제
                "OPTIONS"   // CORS Preflight
        ));

        // 실제 사용하는 헤더만 명시적으로 허용
        configuration.setAllowedHeaders(List.of(
                "Authorization",    // JWT 토큰
                "Content-Type",     // application/json, multipart/form-data, text/event-stream
                "Accept",           // 응답 타입 지정
                "Origin",           // CORS 출처
                "X-Requested-With"  // AJAX 요청 식별
        ));

        configuration.setAllowCredentials(true);    // 쿠키 포함 요청 허용
        configuration.setExposedHeaders(EXPOSED_HEADERS); // Authorization 헤더 노출

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration(CORS_PATH_PATTERN, configuration);
        return source;
    }
}
