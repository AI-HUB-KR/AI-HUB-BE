package kr.ai_hub.AI_HUB_BE.wallet.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.ai_hub.AI_HUB_BE.wallet.service.UserWalletService;
import kr.ai_hub.AI_HUB_BE.wallet.dto.BalanceResponse;
import kr.ai_hub.AI_HUB_BE.wallet.dto.UserWalletResponse;
import kr.ai_hub.AI_HUB_BE.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "사용자 지갑", description = "사용자 지갑 및 잔액 관리")
@Slf4j
@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
public class UserWalletController {

    private final UserWalletService userWalletService;

    /**
     * 현재 사용자의 지갑 상세 정보를 조회합니다.
     */
    @Operation(summary = "지갑 상세 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<UserWalletResponse>> getUserWallet() {
        log.info("지갑 조회 API 호출");

        UserWalletResponse response = userWalletService.getUserWallet();

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * 현재 사용자의 코인 잔액만 조회합니다.
     */
    @Operation(summary = "코인 잔액 조회")
    @GetMapping("/balance")
    public ResponseEntity<ApiResponse<BalanceResponse>> getBalance() {
        log.info("잔액 조회 API 호출");

        BalanceResponse response = userWalletService.getBalance();

        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
