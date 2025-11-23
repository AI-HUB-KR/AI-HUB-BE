package kr.ai_hub.AI_HUB_BE.controller.payment;

import kr.ai_hub.AI_HUB_BE.application.payment.PaymentHistoryService;
import kr.ai_hub.AI_HUB_BE.application.payment.dto.PaymentResponse;
import kr.ai_hub.AI_HUB_BE.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentHistoryController {

    private final PaymentHistoryService paymentHistoryService;

    /**
     * 현재 사용자의 결제 내역을 페이지네이션하여 조회합니다.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> getPayments(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("결제 내역 조회 API 호출: status={}, page={}, size={}", status, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<PaymentResponse> response = paymentHistoryService.getPayments(status, pageable);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * 특정 결제의 상세 정보를 조회합니다.
     */
    @GetMapping("/{paymentId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPayment(@PathVariable Long paymentId) {
        log.info("결제 상세 조회 API 호출: paymentId={}", paymentId);

        PaymentResponse response = paymentHistoryService.getPayment(paymentId);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
