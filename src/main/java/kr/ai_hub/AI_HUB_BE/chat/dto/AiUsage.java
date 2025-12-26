package kr.ai_hub.AI_HUB_BE.chat.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

/**
 * AI 서버 토큰 사용량 정보
 *
 * @param inputTokens 입력 토큰 수
 * @param outputTokens 출력 토큰 수
 * @param totalTokens 총 토큰 수
 */
@Builder
public record AiUsage(
        @JsonProperty("input_tokens")
        Integer inputTokens,

        @JsonProperty("output_tokens")
        Integer outputTokens,

        @JsonProperty("total_tokens")
        Integer totalTokens
) {
}
