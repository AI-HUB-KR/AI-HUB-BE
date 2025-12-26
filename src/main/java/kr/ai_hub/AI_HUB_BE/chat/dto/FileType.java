package kr.ai_hub.AI_HUB_BE.chat.dto;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 파일 타입 Enum
 * <p>
 * IMAGE: 이미지 파일
 * DOCUMENT: 문서 파일
 * AUDIO: 오디오 파일
 * </p>
 */
@Getter
@RequiredArgsConstructor
public enum FileType {
    IMAGE("image"),
    DOCUMENT("document"),
    AUDIO("audio");

    private final String value;

    @JsonValue
    public String getValue() {
        return value;
    }
}
