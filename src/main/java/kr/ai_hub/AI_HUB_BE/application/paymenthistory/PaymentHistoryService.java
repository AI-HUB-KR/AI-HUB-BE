package kr.ai_hub.AI_HUB_BE.application.paymenthistory;

import kr.ai_hub.AI_HUB_BE.application.paymenthistory.dto.PaymentResponse;
import kr.ai_hub.AI_HUB_BE.domain.paymenthistory.entity.PaymentHistory;
import kr.ai_hub.AI_HUB_BE.domain.paymenthistory.repository.PaymentHistoryRepository;
import kr.ai_hub.AI_HUB_BE.domain.user.entity.User;
import kr.ai_hub.AI_HUB_BE.domain.user.repository.UserRepository;
import kr.ai_hub.AI_HUB_BE.global.error.exception.ForbiddenException;
import kr.ai_hub.AI_HUB_BE.global.error.exception.PaymentNotFoundException;
import kr.ai_hub.AI_HUB_BE.global.error.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentHistoryService {

    private final PaymentHistoryRepository paymentHistoryRepository;
    private final UserRepository userRepository;

    /**
     * 현재 사용자의 결제 내역을 페이지네이션하여 조회합니다.
     */
    public Page<PaymentResponse> getPayments(String status, Pageable pageable) {
        Integer userId = getCurrentUserId();
        log.debug("사용자 {} 결제 내역 조회 (status={}, page={}, size={})",
                userId, status, pageable.getPageNumber(), pageable.getPageSize());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + userId));

        Page<PaymentHistory> payments;
        if (status != null && !status.isBlank()) {
            payments = paymentHistoryRepository.findByUserAndStatus(user, status, pageable);
        } else {
            payments = paymentHistoryRepository.findByUser(user, pageable);
        }

        return payments.map(PaymentResponse::from);
    }

    /**
     * 특정 결제의 상세 정보를 조회합니다.
     */
    public PaymentResponse getPayment(Long paymentId) {
        Integer userId = getCurrentUserId();
        log.debug("결제 {} 상세 조회 by 사용자 {}", paymentId, userId);

        PaymentHistory payment = paymentHistoryRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("결제 내역을 찾을 수 없습니다: " + paymentId));

        // 권한 확인: 결제 소유자만 조회 가능
        if (!payment.getUser().getUserId().equals(userId)) {
            log.warn("결제 접근 권한 없음: paymentId={}, userId={}", paymentId, userId);
            throw new ForbiddenException("해당 결제 내역에 접근할 권한이 없습니다");
        }

        return PaymentResponse.from(payment);
    }

    /**
     * SecurityContext에서 현재 인증된 사용자의 ID를 가져옵니다.
     */
    private Integer getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UserNotFoundException("인증되지 않은 사용자입니다");
        }

        try {
            return Integer.parseInt(authentication.getName());
        } catch (NumberFormatException e) {
            log.error("유효하지 않은 사용자 ID 형식: {}", authentication.getName());
            throw new UserNotFoundException("유효하지 않은 사용자 ID입니다");
        }
    }
}
