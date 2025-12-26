package kr.ai_hub.AI_HUB_BE.application.payment.dto;

import kr.ai_hub.AI_HUB_BE.domain.payment.PaymentHistory;
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
        Long paymentId,
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
    public static PaymentResponse from(PaymentHistory payment) {
        return PaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .transactionId(payment.getTransactionId())
                .paymentMethod(payment.getPaymentMethod())
                .payAmountKrw(payment.getPayAmountKrw())
                .payAmountUsd(payment.getPayAmountUsd())
                .paidCoin(payment.getPaidCoin())
                .promotionCoin(payment.getPromotionCoin())
                .status(payment.getStatus())
                .paymentGateway(payment.getPaymentGateway())
                .metadata(payment.getMetadata())
                .createdAt(payment.getCreatedAt().toInstant(ZoneOffset.UTC))
                .completedAt(payment.getCompletedAt() != null ?
                        payment.getCompletedAt().toInstant(ZoneOffset.UTC) : null)
                .build();
    }
}
