package kr.ai_hub.AI_HUB_BE.global.auth.userinfo;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class OAuth2UserInfoFactory {

    public static OAuth2UserInfo getOAuth2UserInfo(
            String registrationId, Map<String,Object> attributes) {
        if ("kakao".equalsIgnoreCase(registrationId)) {
            return new KakaoOAuth2UserInfo(attributes);
        }
        throw new IllegalArgumentException(
                "지원하지 않는 OAuth2 공급자: " + registrationId);
    }
}