package kr.ai_hub.AI_HUB_BE.global.config;

import kr.ai_hub.AI_HUB_BE.application.auth.CustomOAuth2UserService;
import kr.ai_hub.AI_HUB_BE.global.auth.jwt.JwtAuthenticationEntryPoint;
import kr.ai_hub.AI_HUB_BE.global.auth.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService oAuth2UserService;
    private final AuthenticationSuccessHandler oAuth2SuccessHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Value("${deployment.address}")
    private String deploymentAddress;

    @Value("${cors.allowed-origins}")
    private String[] allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // CORS 설정 추가
                .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.disable())) // H2 Console iframe 허용
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/h2-console/**").permitAll() // H2 Console 허용
                        .requestMatchers("/login", "/oauth2/**", "/ws/info/**", "/api/token/refresh",
                                "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/favicon.ico", "/actuator/**").permitAll()//허용하는 경로
                        // 모든 OPTIONS 요청 허용 (CORS preflight)
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .userInfoEndpoint(u -> u.userService(oAuth2UserService))
                        .successHandler(oAuth2SuccessHandler)
                )
                // ↓ JWT 검증 필터 등록
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // ↓ (선택) 인증 실패 핸들러
                .exceptionHandling(ex ->
                        ex.authenticationEntryPoint(jwtAuthenticationEntryPoint));

        return http.build();
    }
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 설정 파일에서 읽어온 allowed origins 추가 (프로필별로 다르게 설정 가능)
        for (String origin : allowedOrigins) {
            configuration.addAllowedOrigin(origin.trim());
        }

        configuration.addAllowedMethod("*"); // 모든 HTTP 메서드 허용 (GET, POST, PUT, DELETE 등)
        configuration.addAllowedHeader("*"); // 모든 헤더 허용
        configuration.setAllowCredentials(true); // 쿠키 포함 요청 허용 (credentials: include)
        configuration.setExposedHeaders(java.util.List.of("Authorization")); // Authorization 응답 헤더 노출

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // 모든 경로에 대해 적용
        return source;
    }
}