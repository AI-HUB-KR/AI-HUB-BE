package kr.ai_hub.AI_HUB_BE.application.chat.message.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

/**
 * AI 서버 공통 응답 래퍼
 *
 * @param success 성공 여부
 * @param data 응답 데이터
 * @param error 에러 정보 (실패 시)
 */
@Builder
public record AiServerResponse<T>(
        boolean success,
        T data,
        ErrorInfo error
) {
    @Builder
    public record ErrorInfo(
            String code,
            String message
    ) {
    }
}
