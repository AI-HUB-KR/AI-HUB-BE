package kr.ai_hub.AI_HUB_BE.controller.admin.dto;

import jakarta.validation.constraints.NotNull;
import kr.ai_hub.AI_HUB_BE.domain.user.UserRole;

/**
 * 사용자 권한 수정 요청 DTO
 */
public record ModifyUserAuthorityRequest(
        @NotNull(message = "사용자 ID는 필수입니다")
        Integer userId,

        @NotNull(message = "권한은 필수입니다")
        UserRole role
) {
}
