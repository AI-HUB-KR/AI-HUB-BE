package kr.ai_hub.AI_HUB_BE.chat.dto;

import kr.ai_hub.AI_HUB_BE.chat.domain.Message;
import lombok.Builder;

/**
 * AI 서버로 전송할 대화 히스토리 메시지 DTO
 *
 * @param role    역할: user, assistant
 * @param content 메시지 내용
 */
@Builder
public record ChatHistoryMessage(
        String role,
        String content
) {
    /**
     * Message 엔티티로부터 ChatHistoryMessage 생성
     *
     * @param message Message 엔티티
     * @return ChatHistoryMessage
     */
    public static ChatHistoryMessage from(Message message) {
        return ChatHistoryMessage.builder()
                .role(message.getRole().getValue())
                .content(message.getContent())
                .build();
    }
}
