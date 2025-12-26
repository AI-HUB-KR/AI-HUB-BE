package kr.ai_hub.AI_HUB_BE.dashboard.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 일별 사용량 상세 DTO
 */
@Builder
public record DailyUsageDetail(
        LocalDate date,
        BigDecimal coinUsed,
        Long messageCount
) {}
