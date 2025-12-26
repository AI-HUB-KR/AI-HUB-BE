package kr.ai_hub.AI_HUB_BE.wallet.service;

import kr.ai_hub.AI_HUB_BE.wallet.dto.CoinTransactionResponse;
import kr.ai_hub.AI_HUB_BE.wallet.domain.CoinTransaction;
import kr.ai_hub.AI_HUB_BE.wallet.domain.CoinTransactionRepository;
import kr.ai_hub.AI_HUB_BE.user.domain.User;
import kr.ai_hub.AI_HUB_BE.user.domain.UserRepository;
import kr.ai_hub.AI_HUB_BE.global.security.SecurityContextHelper;
import kr.ai_hub.AI_HUB_BE.global.error.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CoinTransactionService {

    private final CoinTransactionRepository coinTransactionRepository;
    private final UserRepository userRepository;
    private final SecurityContextHelper securityContextHelper;

    /**
     * 현재 사용자의 코인 거래 내역을 필터링하여 조회합니다.
     */
    public Page<CoinTransactionResponse> getTransactions(
            String transactionType,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable) {
        Integer userId = securityContextHelper.getCurrentUserId();
        log.debug("사용자 {} 코인 거래 내역 조회 (type={}, startDate={}, endDate={}, page={}, size={})",
                userId, transactionType, startDate, endDate, pageable.getPageNumber(), pageable.getPageSize());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + userId));

        Page<CoinTransaction> transactions;

        // 날짜 범위를 LocalDateTime으로 변환 (시작일 00:00:00 ~ 종료일 23:59:59)
        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = endDate != null ? endDate.atTime(LocalTime.MAX) : null;

        // 필터 조합에 따라 적절한 쿼리 메서드 호출
        if (transactionType != null && !transactionType.isBlank() && startDateTime != null && endDateTime != null) {
            // 타입 + 날짜 필터
            transactions = coinTransactionRepository.findByUserAndTransactionTypeAndCreatedAtBetween(
                    user, transactionType, startDateTime, endDateTime, pageable);
        } else if (transactionType != null && !transactionType.isBlank()) {
            // 타입 필터만
            transactions = coinTransactionRepository.findByUserAndTransactionType(user, transactionType, pageable);
        } else if (startDateTime != null && endDateTime != null) {
            // 날짜 필터만
            transactions = coinTransactionRepository.findByUserAndCreatedAtBetween(
                    user, startDateTime, endDateTime, pageable);
        } else {
            // 필터 없음
            transactions = coinTransactionRepository.findByUser(user, pageable);
        }

        return transactions.map(CoinTransactionResponse::from);
    }
}
