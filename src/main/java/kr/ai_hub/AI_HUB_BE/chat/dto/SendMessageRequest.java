package kr.ai_hub.AI_HUB_BE.chat.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.List;

/**
 * 메시지 전송 요청 DTO
 *
 * @param message            사용자 메시지 내용 (필수)
 * @param modelId            AI 모델 ID (필수)
 * @param files              첨부 파일 목록 (선택)
 */
@Builder
public record SendMessageRequest(
        @NotBlank(message = "메시지는 필수입니다")
        String message,

        @NotNull(message = "모델 ID는 필수입니다")
        Integer modelId,

        @Valid
        List<FileAttachment> files
) {
}
