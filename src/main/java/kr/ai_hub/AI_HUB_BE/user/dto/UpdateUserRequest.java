package kr.ai_hub.AI_HUB_BE.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @NotBlank(message = "사용자 이름은 필수입니다")
        @Size(min = 2, max = 30, message = "사용자 이름은 2자 이상 30자 이하여야 합니다")
        String username,

        @NotBlank(message = "이메일은 필수입니다")
        @Email(message = "올바른 이메일 형식이 아닙니다")
        String email
) {
}
