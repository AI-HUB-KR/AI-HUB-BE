package kr.ai_hub.AI_HUB_BE.dashboard.dto;

import kr.ai_hub.AI_HUB_BE.aimodel.domain.AIModel;
import lombok.Builder;

import java.math.BigDecimal;

/**
 * 모델 가격 대시보드 응답 DTO
 */
@Builder
public record ModelPricingResponse(
        Integer modelId,
        String modelName,
        String displayName,
        BigDecimal inputPricePer1m,
        BigDecimal outputPricePer1m,
        Boolean isActive
) {
    public static ModelPricingResponse from(AIModel model) {
        return ModelPricingResponse.builder()
                .modelId(model.getModelId())
                .modelName(model.getModelName())
                .displayName(model.getDisplayName())
                .inputPricePer1m(model.getInputPricePer1m())
                .outputPricePer1m(model.getOutputPricePer1m())
                .isActive(model.getIsActive())
                .build();
    }
}
