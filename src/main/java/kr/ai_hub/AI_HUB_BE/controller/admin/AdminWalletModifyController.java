package kr.ai_hub.AI_HUB_BE.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.ai_hub.AI_HUB_BE.application.admin.AdminWalletModifyService;
import kr.ai_hub.AI_HUB_BE.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@Tag(name = "지갑 관리 (관리자)", description = "사용자 지갑 잔액 수정")
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/wallet")
@RequiredArgsConstructor
public class AdminWalletModifyController {

    private final AdminWalletModifyService adminWalletModifyService;

    @Operation(summary = "사용자 지갑 잔액 수정")
    @PatchMapping
    public ResponseEntity<ApiResponse<Void>> modifyUserWallet(
            @RequestParam Long userId,
            @RequestParam Integer amount) {
        log.info("지갑 수정 API 호출: userId={}, amount={}", userId, amount);
        adminWalletModifyService.setUserBalance(userId.intValue(), BigDecimal.valueOf(amount));
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.ok());
    }
}
