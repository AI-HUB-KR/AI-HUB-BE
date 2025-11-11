package kr.ai_hub.AI_HUB_BE.application.cointransaction.dto;

import kr.ai_hub.AI_HUB_BE.domain.cointransaction.entity.CoinTransaction;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;

/**
 * 코인 거래 내역 응답 DTO
 */
@Builder
public record CoinTransactionResponse(
        Long transactionId,
        String transactionType,
        BigDecimal amount,
        BigDecimal balanceAfter,
        String description,
        Integer modelId,
        String modelName,
        String roomId,
        String messageId,
        Instant createdAt
) {
    public static CoinTransactionResponse from(CoinTransaction transaction) {
        return CoinTransactionResponse.builder()
                .transactionId(transaction.getTransactionId())
                .transactionType(transaction.getTransactionType())
                .amount(transaction.getAmount())
                .balanceAfter(transaction.getBalanceAfter())
                .description(transaction.getDescription())
                .modelId(transaction.getAiModel() != null ? transaction.getAiModel().getModelId() : null)
                .modelName(transaction.getAiModel() != null ? transaction.getAiModel().getModelName() : null)
                .roomId(transaction.getChatRoom() != null ? transaction.getChatRoom().getRoomId().toString() : null)
                .messageId(transaction.getMessage() != null ? transaction.getMessage().getMessageId().toString() : null)
                .createdAt(transaction.getCreatedAt().toInstant(ZoneOffset.UTC))
                .build();
    }
}
