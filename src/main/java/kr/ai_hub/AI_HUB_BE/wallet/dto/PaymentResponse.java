package kr.ai_hub.AI_HUB_BE.wallet.dto;

import kr.ai_hub.AI_HUB_BE.wallet.domain.WalletHistory;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

/**
 * 결제 내역 응답 DTO
 */
@Builder
public record PaymentResponse(
        Long historyId,
        String transactionId,
        String paymentMethod,
        BigDecimal payAmountKrw,
        BigDecimal payAmountUsd,
        BigDecimal paidCoin,
        BigDecimal promotionCoin,
        String status,
        String paymentGateway,
        Map<String, Object> metadata,
        Instant createdAt,
        Instant completedAt
) {
    public static PaymentResponse from(WalletHistory history) {
        return PaymentResponse.builder()
                .historyId(history.getHistoryId())
                .transactionId(history.getTransactionId())
                .paymentMethod(history.getPaymentMethod())
                .payAmountKrw(history.getPayAmountKrw())
                .payAmountUsd(history.getPayAmountUsd())
                .paidCoin(history.getPaidCoin())
                .promotionCoin(history.getPromotionCoin())
                .status(history.getStatus())
                .paymentGateway(history.getPaymentGateway())
                .metadata(history.getMetadata())
                .createdAt(history.getCreatedAt().toInstant(ZoneOffset.UTC))
                .completedAt(history.getCompletedAt() != null ?
                        history.getCompletedAt().toInstant(ZoneOffset.UTC) : null)
                .build();
    }
}
