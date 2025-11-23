package kr.ai_hub.AI_HUB_BE.application.admin.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * 관리자 AI 모델 생성 요청 DTO
 */
public record CreateAIModelRequest(
        @NotBlank(message = "모델 이름은 필수입니다")
        @Pattern(regexp = "^[a-z0-9-]+$", message = "모델 이름은 소문자, 숫자, 하이픈만 사용할 수 있습니다")
        String modelName,

        @NotBlank(message = "표시 이름은 필수입니다")
        @Size(max = 30, message = "표시 이름은 최대 30자입니다")
        String displayName,

        @Size(max = 200, message = "설명은 최대 200자입니다")
        String displayExplain,

        @NotNull(message = "입력 가격은 필수입니다")
        @DecimalMin(value = "0.0", message = "입력 가격은 0 이상이어야 합니다")
        BigDecimal inputPricePer1m,

        @NotNull(message = "출력 가격은 필수입니다")
        @DecimalMin(value = "0.0", message = "출력 가격은 0 이상이어야 합니다")
        BigDecimal outputPricePer1m,

        @NotNull(message = "활성화 여부는 필수입니다")
        Boolean isActive
) {}
