package kr.ai_hub.AI_HUB_BE.auth.domain;

// 토큰 폐기 사유를 정의한다.
public enum TokenRevokeReason {
    ROTATED,
    EXPIRED,
    USER_LOGOUT;

    public String value() {
        return name();
    }
}
