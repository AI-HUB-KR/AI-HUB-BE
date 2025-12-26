package kr.ai_hub.AI_HUB_BE.chat.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

/**
 * AI 서버 파일 업로드 응답 데이터
 *
 * @param fileId 업로드된 파일 ID
 */
@Builder
public record AiUploadData(
        @JsonProperty("file_id")
        String fileId
) {
}
