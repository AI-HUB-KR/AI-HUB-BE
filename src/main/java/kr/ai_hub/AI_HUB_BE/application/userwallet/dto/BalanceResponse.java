package kr.ai_hub.AI_HUB_BE.application.userwallet.dto;

import lombok.Builder;

import java.math.BigDecimal;

/**
 * 잔액 조회 응답 DTO
 */
@Builder
public record BalanceResponse(
        BigDecimal balance
) {}
