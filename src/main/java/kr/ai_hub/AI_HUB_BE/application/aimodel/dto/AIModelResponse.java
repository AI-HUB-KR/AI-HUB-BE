package kr.ai_hub.AI_HUB_BE.application.aimodel.dto;

import kr.ai_hub.AI_HUB_BE.domain.aimodel.AIModel;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;

@Builder
public record AIModelResponse(
        Integer modelId,
        String modelName,
        String displayName,
        String displayExplain,
        BigDecimal inputPricePer1m,
        BigDecimal outputPricePer1m,
        Boolean isActive,
        Instant createdAt,
        Instant updatedAt
) {
    public static AIModelResponse from(AIModel model) {
        return AIModelResponse.builder()
                .modelId(model.getModelId())
                .modelName(model.getModelName())
                .displayName(model.getDisplayName())
                .displayExplain(model.getDisplayExplain())
                .inputPricePer1m(model.getInputPricePer1m())
                .outputPricePer1m(model.getOutputPricePer1m())
                .isActive(model.getIsActive())
                .createdAt(model.getCreatedAt().toInstant(ZoneOffset.UTC))
                .updatedAt(model.getUpdatedAt().toInstant(ZoneOffset.UTC))
                .build();
    }
}
