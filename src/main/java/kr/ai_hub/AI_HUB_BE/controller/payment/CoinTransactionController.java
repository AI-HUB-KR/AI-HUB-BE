package kr.ai_hub.AI_HUB_BE.controller.payment;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.ai_hub.AI_HUB_BE.application.payment.CoinTransactionService;
import kr.ai_hub.AI_HUB_BE.application.payment.dto.CoinTransactionResponse;
import kr.ai_hub.AI_HUB_BE.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(name = "코인 거래", description = "코인 거래 내역 조회")
@Slf4j
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class CoinTransactionController {

    private final CoinTransactionService coinTransactionService;

    /**
     * 현재 사용자의 코인 거래 내역을 필터링하여 조회합니다.
     */
    @Operation(summary = "코인 거래 내역 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<CoinTransactionResponse>>> getTransactions(
            @RequestParam(required = false) String transactionType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("코인 거래 내역 조회 API 호출: type={}, startDate={}, endDate={}, page={}, size={}",
                transactionType, startDate, endDate, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<CoinTransactionResponse> response = coinTransactionService.getTransactions(
                transactionType, startDate, endDate, pageable);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
