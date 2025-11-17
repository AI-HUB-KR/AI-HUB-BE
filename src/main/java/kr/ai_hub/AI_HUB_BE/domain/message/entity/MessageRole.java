package kr.ai_hub.AI_HUB_BE.domain.message.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 메시지 역할 Enum
 * <p>
 * USER: 사용자가 보낸 메시지
 * ASSISTANT: AI가 응답한 메시지
 * </p>
 */
@Getter
@RequiredArgsConstructor
public enum MessageRole {
    USER("user"),
    ASSISTANT("assistant");

    private final String value;

    /**
     * 문자열 값으로부터 MessageRole을 찾습니다.
     *
     * @param value "user" 또는 "assistant"
     * @return MessageRole
     * @throws IllegalArgumentException 지원하지 않는 값인 경우
     */
    public static MessageRole fromValue(String value) {
        for (MessageRole role : values()) {
            if (role.value.equals(value)) {
                return role;
            }
        }
        throw new IllegalArgumentException("지원하지 않는 MessageRole 값입니다: " + value);
    }
}
