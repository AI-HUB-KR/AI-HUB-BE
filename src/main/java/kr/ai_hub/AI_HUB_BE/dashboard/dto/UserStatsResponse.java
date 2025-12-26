package kr.ai_hub.AI_HUB_BE.dashboard.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 사용자 통계 요약 응답 DTO
 */
@Builder
public record UserStatsResponse(
        BigDecimal totalCoinPurchased,
        BigDecimal totalCoinUsed,
        BigDecimal currentBalance,
        Long totalMessages,
        Long totalChatRooms,
        MostUsedModel mostUsedModel,
        BigDecimal last30DaysUsage,
        Instant memberSince
) {}
