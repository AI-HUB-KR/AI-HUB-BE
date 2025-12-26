package kr.ai_hub.AI_HUB_BE.dashboard.dto;

import lombok.Builder;

/**
 * 가장 많이 사용한 모델 DTO
 */
@Builder
public record MostUsedModel(
        Integer modelId,
        String modelName,
        String displayName,
        Double usagePercentage
) {}
