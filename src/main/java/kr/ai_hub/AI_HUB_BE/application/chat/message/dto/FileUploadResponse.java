package kr.ai_hub.AI_HUB_BE.application.chat.message.dto;

import lombok.Builder;

/**
 * 파일 업로드 응답 DTO
 *
 * @param fileId AI 서버에서 반환된 파일 ID
 */
@Builder
public record FileUploadResponse(
        String fileId
) {
    public static FileUploadResponse of(String fileId) {
        return FileUploadResponse.builder()
                .fileId(fileId)
                .build();
    }
}
