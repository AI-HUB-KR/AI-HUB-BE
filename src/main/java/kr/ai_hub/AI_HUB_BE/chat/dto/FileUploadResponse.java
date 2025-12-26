package kr.ai_hub.AI_HUB_BE.chat.dto;

import lombok.Builder;

/**
 * 파일 업로드 응답 DTO
 *
 * @param fileId AI 서버에서 반환된 파일 ID
 * TODO: 향후 파일 메타데이터(이름, 크기 등), ClaudeFlare R2 연동 시 URL 등 추가 고려
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
