package kr.ai_hub.AI_HUB_BE.global.auth.userinfo;

import java.util.Map;

public class KakaoOAuth2UserInfo implements OAuth2UserInfo {
    private final Map<String, Object> attrs;
    private final Map<String, Object> account;
    private final Map<String, Object> profile;

    public KakaoOAuth2UserInfo(Map<String, Object> attrs) {
        this.attrs = attrs;
        this.account = (Map<String, Object>) attrs.get("kakao_account");
        this.profile = (Map<String, Object>) account.get("profile");
    }

    @Override
    public String getId() {
        return attrs.get("id").toString();
    }

    @Override
    public String getEmail() {
        return account.get("email").toString();
    }

    @Override
    public String getName() {
        return profile.get("nickname").toString();
    }

    @Override
    public String getProfileImageUrl() {
        return profile.get("profile_image_url").toString();
    }

    public Map<String, Object> getAttributes() {
        return attrs;
    }
}