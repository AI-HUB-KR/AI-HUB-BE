package kr.ai_hub.AI_HUB_BE.global.security;

import kr.ai_hub.AI_HUB_BE.global.security.oauth2.CustomOAuth2User;
import kr.ai_hub.AI_HUB_BE.user.domain.User;
import kr.ai_hub.AI_HUB_BE.user.domain.UserRole;
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

        CustomOAuth2User principal = new CustomOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(annotation.role())),
                Map.of("id", "kakao_12345"),
                "id",
                user);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "kakao"));
        return context;
    }
}
