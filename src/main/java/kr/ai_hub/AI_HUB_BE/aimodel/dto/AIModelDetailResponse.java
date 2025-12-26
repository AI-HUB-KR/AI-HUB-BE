package kr.ai_hub.AI_HUB_BE.aimodel.dto;

import kr.ai_hub.AI_HUB_BE.aimodel.domain.AIModel;
import lombok.Builder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneOffset;

@Builder
public record AIModelDetailResponse(
        Integer modelId,
        String modelName,
        String displayName,
        String displayExplain,
        BigDecimal inputPricePer1m,
        BigDecimal outputPricePer1m,
        BigDecimal modelMarkupRate,
        Boolean isActive,
        Instant createdAt,
        Instant updatedAt
) {
    public static AIModelDetailResponse from(AIModel model) {
        BigDecimal divisor = BigDecimal.ONE.add(model.getModelMarkupRate());

        return AIModelDetailResponse.builder()
                .modelId(model.getModelId())
                .modelName(model.getModelName())
                .displayName(model.getDisplayName())
                .displayExplain(model.getDisplayExplain())
                .inputPricePer1m(model.getInputPricePer1m() // (1 + markupRate) 으로 나누기
                        .divide(divisor, 10, RoundingMode.HALF_UP))
                .outputPricePer1m(model.getOutputPricePer1m()
                        .divide(divisor, 10, RoundingMode.HALF_UP))
                .modelMarkupRate(model.getModelMarkupRate())
                .isActive(model.getIsActive())
                .createdAt(model.getCreatedAt().toInstant(ZoneOffset.UTC))
                .updatedAt(model.getUpdatedAt().toInstant(ZoneOffset.UTC))
                .build();
    }
}
