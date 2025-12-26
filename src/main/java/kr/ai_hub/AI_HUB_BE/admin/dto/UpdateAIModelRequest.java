package kr.ai_hub.AI_HUB_BE.admin.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 관리자 AI 모델 수정 요청 DTO
 */
public record UpdateAIModelRequest(
        @Size(max = 30, message = "표시 이름은 최대 30자입니다")
        String displayName,

        @Size(max = 200, message = "설명은 최대 200자입니다")
        String displayExplain,

        @DecimalMin(value = "0.0", message = "입력 가격은 0 이상이어야 합니다")
        BigDecimal inputPricePer1m,

        @DecimalMin(value = "0.0", message = "출력 가격은 0 이상이어야 합니다")
        BigDecimal outputPricePer1m,

        @DecimalMin(value ="0.0", inclusive = true, message = "모델 마크업 비율은 0 이상이어야 합니다")
        BigDecimal modelMarkupRate,

        Boolean isActive
) {}
