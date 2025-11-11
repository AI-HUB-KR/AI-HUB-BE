package kr.ai_hub.AI_HUB_BE.application.userwallet.dto;

import kr.ai_hub.AI_HUB_BE.domain.userwallet.entity.UserWallet;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;

/**
 * 사용자 지갑 상세 응답 DTO
 */
@Builder
public record UserWalletResponse(
        Integer walletId,
        Integer userId,
        BigDecimal balance,
        BigDecimal totalPurchased,
        BigDecimal totalUsed,
        Instant lastTransactionAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static UserWalletResponse from(UserWallet wallet) {
        return UserWalletResponse.builder()
                .walletId(wallet.getWalletId())
                .userId(wallet.getUser().getUserId())
                .balance(wallet.getBalance())
                .totalPurchased(wallet.getTotalPurchased())
                .totalUsed(wallet.getTotalUsed())
                .lastTransactionAt(wallet.getLastTransactionAt() != null ?
                        wallet.getLastTransactionAt().toInstant(ZoneOffset.UTC) : null)
                .createdAt(wallet.getCreatedAt().toInstant(ZoneOffset.UTC))
                .updatedAt(wallet.getUpdatedAt().toInstant(ZoneOffset.UTC))
                .build();
    }
}
