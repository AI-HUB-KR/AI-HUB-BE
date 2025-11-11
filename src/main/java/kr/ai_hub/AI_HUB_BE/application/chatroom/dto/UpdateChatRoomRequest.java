package kr.ai_hub.AI_HUB_BE.application.chatroom.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 채팅방 제목 수정 요청 DTO
 */
public record UpdateChatRoomRequest(
        @NotBlank(message = "채팅방 제목은 필수입니다")
        @Size(max = 30, message = "채팅방 제목은 30자 이하여야 합니다")
        String title
) {}
