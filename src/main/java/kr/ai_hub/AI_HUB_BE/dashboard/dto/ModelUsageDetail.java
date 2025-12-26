package kr.ai_hub.AI_HUB_BE.dashboard.dto;

import lombok.Builder;

import java.math.BigDecimal;

/**
 * 모델별 사용량 상세 DTO
 */
@Builder
public record ModelUsageDetail(
        Integer modelId,
        String modelName,
        String displayName,
        BigDecimal coinUsed,
        Long messageCount,
        BigDecimal tokenCount,
        Double percentage
) {}
