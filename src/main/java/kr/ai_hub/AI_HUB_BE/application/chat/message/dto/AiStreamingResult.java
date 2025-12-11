package kr.ai_hub.AI_HUB_BE.application.chat.message.dto;

public record AiStreamingResult(
        String type,
        String aiResponseId,
        String fullContent,
        AiUsage usage
) {
    /**
     * Tpye 기본값 "usage" 생성자
     * @param aiResponseId
     * @param fullContent
     * @param usage
     */
    public AiStreamingResult(String aiResponseId, String fullContent, AiUsage usage) {
        this( "usage", aiResponseId, fullContent, usage);
    }
}
