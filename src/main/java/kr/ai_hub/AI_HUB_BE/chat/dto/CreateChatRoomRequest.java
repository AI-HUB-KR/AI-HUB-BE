package kr.ai_hub.AI_HUB_BE.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 채팅방 생성 요청 DTO
 */
public record CreateChatRoomRequest(
        @NotBlank(message = "채팅방 제목은 필수입니다")
        @Size(max = 30, message = "채팅방 제목은 30자 이하여야 합니다")
        String title,

        @NotNull(message = "AI 모델 ID는 필수입니다")
        Integer modelId
) {}
