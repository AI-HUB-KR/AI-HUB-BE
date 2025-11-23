package kr.ai_hub.AI_HUB_BE.application.chat.message.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

/**
 * AI 서버 공통 응답 래퍼
 *
 * @param success 성공 여부
 * @param data 응답 데이터
 * @param error 에러 정보 (실패 시)
 * @param metadata 메타데이터 (제공자, 타임스탐프 등)
 */
@Builder
public record AiServerResponse<T>(
        boolean success,
        T data,
        ErrorInfo error,
        AiResponseMetadata metadata
) {
    @Builder
    public record ErrorInfo(
            String code,
            String message
    ) {
    }
}

/**
 * AI 서버 응답 메타데이터
 *
 * @param provider AI 제공자 (예: openai, anthropic)
 * @param timestamp 응답 생성 시간 (ISO 8601 형식의 문자열)
 */
@Builder
record AiResponseMetadata(
        String provider,
        String timestamp
) {
}
