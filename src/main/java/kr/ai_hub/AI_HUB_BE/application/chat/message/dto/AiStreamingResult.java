package kr.ai_hub.AI_HUB_BE.application.chat.message.dto;

public record AiStreamingResult(
        String aiResponseId,
        String fullContent,
        AiUsage usage
) {
}
