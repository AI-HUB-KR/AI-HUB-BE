package kr.ai_hub.AI_HUB_BE.global.security.oauth2;

import kr.ai_hub.AI_HUB_BE.global.error.exception.UnsupportedOAuth2ProviderException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class OAuth2UserInfoFactory {

    public static OAuth2UserInfo getOAuth2UserInfo(
            String registrationId, Map<String,Object> attributes) {
        log.debug("OAuth2 제공자 선택: {}", registrationId);

        if ("kakao".equalsIgnoreCase(registrationId)) {
            return new KakaoOAuth2UserInfo(attributes);
        }

        log.error("지원하지 않는 OAuth2 공급자: {}", registrationId);
        throw new UnsupportedOAuth2ProviderException(
                "지원하지 않는 OAuth2 공급자: " + registrationId);
    }
}