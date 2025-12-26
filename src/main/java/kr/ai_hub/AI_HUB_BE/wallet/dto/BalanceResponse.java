package kr.ai_hub.AI_HUB_BE.wallet.dto;

import lombok.Builder;

import java.math.BigDecimal;

/**
 * 잔액 조회 응답 DTO
 */
@Builder
public record BalanceResponse(
        BigDecimal balance,
        BigDecimal paidBalance,
        BigDecimal promotionBalance
) {}
