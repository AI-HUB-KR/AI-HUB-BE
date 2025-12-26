package kr.ai_hub.AI_HUB_BE.controller.admin.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * 관리자 지갑 수정 요청 DTO (프로모션 코인만 수정 가능)
 */
public record ModifyUserWalletRequest(
        @NotNull(message = "사용자 ID는 필수입니다")
        Integer userId,

        @NotNull(message = "프로모션 잔액 변경량은 필수입니다")
        BigDecimal promotionBalance
) {}
