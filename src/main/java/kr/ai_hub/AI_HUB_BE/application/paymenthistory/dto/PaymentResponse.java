package kr.ai_hub.AI_HUB_BE.application.paymenthistory.dto;

import kr.ai_hub.AI_HUB_BE.domain.paymenthistory.entity.PaymentHistory;
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
        BigDecimal amountKrw,
        BigDecimal amountUsd,
        BigDecimal coinAmount,
        BigDecimal bonusCoin,
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
                .amountKrw(payment.getAmountKrw())
                .amountUsd(payment.getAmountUsd())
                .coinAmount(payment.getCoinAmount())
                .bonusCoin(payment.getBonusCoin())
                .status(payment.getStatus())
                .paymentGateway(payment.getPaymentGateway())
                .metadata(payment.getMetadata())
                .createdAt(payment.getCreatedAt().toInstant(ZoneOffset.UTC))
                .completedAt(payment.getCompletedAt() != null ?
                        payment.getCompletedAt().toInstant(ZoneOffset.UTC) : null)
                .build();
    }
}
