package kr.ai_hub.AI_HUB_BE.application.message.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

/**
 * AI 서버 SSE 이벤트
 *
 * @param type 이벤트 타입 (response.created, response.output_text.delta, response.completed, error)
 * @param response 응답 정보 (response.created, response.completed에서 사용)
 * @param delta 증분 텍스트 (response.output_text.delta에서 사용)
 * @param sequenceNumber 시퀀스 번호
 * @param error 에러 정보 (error 타입일 때 사용)
 */
@Builder
public record SseEvent(
        String type,
        ResponseInfo response,
        String delta,
        @JsonProperty("sequence_number")
        Integer sequenceNumber,
        ErrorInfo error
) {
    @Builder
    public record ResponseInfo(
            String id,
            String model,
            String content,
            AiUsage usage
    ) {
    }

    @Builder
    public record ErrorInfo(
            String code,
            String message
    ) {
    }
}
