package kr.ai_hub.AI_HUB_BE.application.message.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

/**
 * AI 서버 채팅 응답 데이터
 *
 * @param id 응답 ID
 * @param content 응답 내용
 * @param model 사용된 모델명
 * @param provider 제공자 (openai, claude 등)
 * @param usage 토큰 사용량 정보
 */
@Builder
public record AiChatData(
        String id,
        String content,
        String model,
        String provider,
        AiUsage usage
) {
}
