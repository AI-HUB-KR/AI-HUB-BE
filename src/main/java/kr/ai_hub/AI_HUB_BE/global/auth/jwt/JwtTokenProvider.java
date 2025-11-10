package kr.ai_hub.AI_HUB_BE.global.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import kr.ai_hub.AI_HUB_BE.domain.user.entity.User;
import kr.ai_hub.AI_HUB_BE.domain.user.repository.UserRepository;
import kr.ai_hub.AI_HUB_BE.global.auth.CustomOauth2User;
import kr.ai_hub.AI_HUB_BE.global.error.exception.InvalidTokenException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final UserRepository userRepository;

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration.access}")
    private long accessValidityInSeconds;

    @Value("${jwt.expiration.refresh}")
    private long refreshValidityInSeconds;

    @PostConstruct
    protected void init() {
        secretKey = Base64.getEncoder().encodeToString(secretKey.getBytes());
    }

    public String createAccessToken(User user) {
        log.debug("사용자 {} 액세스 토큰 생성 중", user.getUserId());

        Claims claims = Jwts.claims()
                .subject(user.getUserId().toString())
                .add("email", user.getEmail())
                .add("role", user.getRole().toString())
                .build();

        Date now = new Date();
        Date validity = new Date(now.getTime() + (accessValidityInSeconds * 1000L));

        String token = Jwts.builder()
                .claims(claims)
                .issuedAt(now)
                .expiration(validity)
                .signWith(Keys.hmacShaKeyFor(secretKey.getBytes()))
                .compact();

        log.debug("사용자 {} 액세스 토큰 생성 완료", user.getUserId());
        return token;
    }

    public String createRefreshToken(User user) {
        log.debug("사용자 {} 리프레시 토큰 생성 중", user.getUserId());

        Claims claims = Jwts.claims()
                .subject(user.getUserId().toString())
                .build();

        Date now = new Date();
        Date validity = new Date(now.getTime() + (refreshValidityInSeconds * 1000L));

        String token = Jwts.builder()
                .claims(claims)
                .issuedAt(now)
                .expiration(validity)
                .signWith(Keys.hmacShaKeyFor(secretKey.getBytes()))
                .compact();

        log.debug("사용자 {} 리프레시 토큰 생성 완료", user.getUserId());
        return token;
    }

    public Authentication getAuthentication(Claims claims) {
        String userId = claims.getSubject();

        // DB에서 User 엔티티 조회
        User user = userRepository.findById(Integer.parseInt(userId))
                .orElseThrow(() -> new InvalidTokenException("존재하지 않는 사용자입니다"));

        // CustomOauth2User 생성
        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority(user.getRole().toString())
        );

        Map<String, Object> attributes = Map.of(
                "userId", user.getUserId(),
                "email", user.getEmail()
        );

        CustomOauth2User principal = new CustomOauth2User(
                authorities,
                attributes,
                "userId",
                user
        );

        return new UsernamePasswordAuthenticationToken(principal, "", authorities);
    }

    // JWT 파싱 및 검증
    public Claims parseClaims(String token) {
        try {
            log.debug("JWT 토큰 파싱 중");
            Claims claims = Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(secretKey.getBytes()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            log.debug("사용자 {} JWT 토큰 파싱 성공", claims.getSubject());
            return claims;
        } catch (Exception e) {
            log.error("JWT 토큰 파싱 실패: {}", e.getMessage());
            throw e;
        }
    }
}
