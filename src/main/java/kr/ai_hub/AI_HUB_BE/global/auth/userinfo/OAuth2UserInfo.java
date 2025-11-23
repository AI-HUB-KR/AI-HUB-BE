package kr.ai_hub.AI_HUB_BE.global.auth.userinfo;

import java.util.Map;

public interface OAuth2UserInfo {
    String getId();
    String getEmail();
    String getName();
    String getProfileImageUrl();
    Map<String, Object> getAttributes();
}