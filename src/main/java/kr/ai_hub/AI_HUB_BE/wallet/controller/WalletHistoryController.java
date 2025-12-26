package kr.ai_hub.AI_HUB_BE.wallet.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.ai_hub.AI_HUB_BE.wallet.service.WalletHistoryService;
import kr.ai_hub.AI_HUB_BE.wallet.dto.PaymentResponse;
import kr.ai_hub.AI_HUB_BE.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "결제 내역", description = "코인 충전 및 결제 내역 관리")
@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class WalletHistoryController {

    private final WalletHistoryService walletHistoryService;

    /**
     * 현재 사용자의 결제 내역을 페이지네이션하여 조회합니다.
     */
    @Operation(summary = "결제 내역 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> getPayments(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("결제 내역 조회 API 호출: status={}, page={}, size={}", status, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<PaymentResponse> response = walletHistoryService.getPayments(status, pageable);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * 특정 결제의 상세 정보를 조회합니다.
     */
    @Operation(summary = "결제 상세 조회")
    @GetMapping("/{paymentId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPayment(@PathVariable Long paymentId) {
        log.info("결제 상세 조회 API 호출: paymentId={}", paymentId);

        PaymentResponse response = walletHistoryService.getPayment(paymentId);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
