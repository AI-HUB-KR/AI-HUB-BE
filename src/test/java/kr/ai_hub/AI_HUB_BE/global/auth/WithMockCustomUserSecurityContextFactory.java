package kr.ai_hub.AI_HUB_BE.global.auth;

import kr.ai_hub.AI_HUB_BE.domain.user.User;
import kr.ai_hub.AI_HUB_BE.domain.user.UserRole;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.Collections;
import java.util.Map;

public class WithMockCustomUserSecurityContextFactory implements WithSecurityContextFactory<WithMockCustomUser> {

    @Override
    public SecurityContext createSecurityContext(WithMockCustomUser annotation) {
        User user = User.builder()
                .userId(1)
                .username(annotation.username())
                .role(UserRole.valueOf(annotation.role()))
                .build();

        CustomOauth2User principal = new CustomOauth2User(
                Collections.singleton(new SimpleGrantedAuthority(annotation.role())),
                Map.of("id", "kakao_12345"),
                "id",
                user);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "kakao"));
        return context;
    }
}
