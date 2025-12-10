package kr.ai_hub.AI_HUB_BE.application.chat.message.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

/**
 * AI 서버 SSE 이벤트
 * <p>
 * AI 서버로부터 수신하는 두 가지 타입의 이벤트:
 * 1. response: 텍스트 응답 스트리밍 ({"type":"response","data":"텍스트 조각"})
 * 2. usage: 사용량 메타데이터 ({"type":"usage","data":{...}})
 * </p>
 *
 * @param type 이벤트 타입 (response, usage)
 * @param data 이벤트 데이터 (response일 때 String, usage일 때 UsageData 객체)
 */
@Builder
public record SseEvent(
        String type,
        Object data
) {
    /**
     * 사용량 메타데이터 DTO
     */
    @Builder
    public record UsageData(
            @JsonProperty("input_tokens")
            Integer inputTokens,

            @JsonProperty("output_tokens")
            Integer outputTokens,

            @JsonProperty("total_tokens")
            Integer totalTokens,

            @JsonProperty("response_id")
            String responseId
    ) {
    }
}
