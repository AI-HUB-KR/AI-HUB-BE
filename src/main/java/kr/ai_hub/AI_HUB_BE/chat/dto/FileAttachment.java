package kr.ai_hub.AI_HUB_BE.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

/**
 * 파일 첨부 정보 DTO
 *
 * @param id   업로드된 파일 ID (필수)
 * @param type 파일 타입: image, document, audio (필수)
 */
@Builder
public record FileAttachment(
        @NotBlank(message = "파일 ID는 필수입니다")
        String id,

        @NotNull(message = "파일 타입은 필수입니다")
        FileType type
) {
}
