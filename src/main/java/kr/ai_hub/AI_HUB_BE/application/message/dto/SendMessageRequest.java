package kr.ai_hub.AI_HUB_BE.application.message.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

/**
 * 메시지 전송 요청 DTO
 *
 * @param message 사용자 메시지 내용
 * @param modelId AI 모델 ID
 * @param fileId 업로드된 파일 ID (optional)
 * @param previousResponseId 이전 응답 ID (대화 이어가기, optional)
 */
@Builder
public record SendMessageRequest(
        @NotBlank(message = "메시지는 필수입니다")
        String message,

        @NotNull(message = "모델 ID는 필수입니다")
        Integer modelId,

        String fileId,

        String previousResponseId
) {
}
