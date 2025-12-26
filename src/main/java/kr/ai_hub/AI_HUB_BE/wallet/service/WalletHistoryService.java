package kr.ai_hub.AI_HUB_BE.wallet.service;

import kr.ai_hub.AI_HUB_BE.wallet.dto.PaymentResponse;
import kr.ai_hub.AI_HUB_BE.wallet.domain.WalletHistory;
import kr.ai_hub.AI_HUB_BE.wallet.domain.WalletHistoryRepository;
import kr.ai_hub.AI_HUB_BE.user.domain.User;
import kr.ai_hub.AI_HUB_BE.user.domain.UserRepository;
import kr.ai_hub.AI_HUB_BE.global.security.SecurityContextHelper;
import kr.ai_hub.AI_HUB_BE.global.error.exception.ForbiddenException;
import kr.ai_hub.AI_HUB_BE.global.error.exception.PaymentNotFoundException;
import kr.ai_hub.AI_HUB_BE.global.error.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WalletHistoryService {

    private final WalletHistoryRepository walletHistoryRepository;
    private final UserRepository userRepository;
    private final SecurityContextHelper securityContextHelper;

    /**
     * 현재 사용자의 결제 내역을 페이지네이션하여 조회합니다.
     */
    public Page<PaymentResponse> getPayments(String status, Pageable pageable) {
        Integer userId = securityContextHelper.getCurrentUserId();
        log.debug("사용자 {} 결제 내역 조회 (status={}, page={}, size={})",
                userId, status, pageable.getPageNumber(), pageable.getPageSize());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + userId));

        Page<WalletHistory> payments;
        if (status != null && !status.isBlank()) {
            payments = walletHistoryRepository.findByUserAndStatus(user, status, pageable);
        } else {
            payments = walletHistoryRepository.findByUser(user, pageable);
        }

        return payments.map(PaymentResponse::from);
    }

    /**
     * 특정 결제의 상세 정보를 조회합니다.
     */
    public PaymentResponse getPayment(Long paymentId) {
        Integer userId = securityContextHelper.getCurrentUserId();
        log.debug("결제 {} 상세 조회 by 사용자 {}", paymentId, userId);

        WalletHistory payment = walletHistoryRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("결제 내역을 찾을 수 없습니다: " + paymentId));

        // 권한 확인: 결제 소유자만 조회 가능
        if (!payment.getUser().getUserId().equals(userId)) {
            log.warn("결제 접근 권한 없음: paymentId={}, userId={}", paymentId, userId);
            throw new ForbiddenException("해당 결제 내역에 접근할 권한이 없습니다");
        }

        return PaymentResponse.from(payment);
    }
}
