package kr.ai_hub.AI_HUB_BE.dashboard.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

/**
 * 월별 모델별 코인 사용량 대시보드 응답 DTO
 */
@Builder
public record MonthlyUsageResponse(
        Integer year,
        Integer month,
        BigDecimal totalCoinUsed,
        List<ModelUsageDetail> modelUsage,
        List<DailyUsageDetail> dailyUsage
) {}
