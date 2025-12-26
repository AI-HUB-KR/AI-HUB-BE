package kr.ai_hub.AI_HUB_BE.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.ai_hub.AI_HUB_BE.admin.service.AdminUserService;
import kr.ai_hub.AI_HUB_BE.admin.dto.ModifyUserAuthorityRequest;
import kr.ai_hub.AI_HUB_BE.admin.dto.ModifyUserWalletRequest;
import kr.ai_hub.AI_HUB_BE.admin.dto.UserListResponse;
import kr.ai_hub.AI_HUB_BE.global.security.SecurityContextHelper;
import kr.ai_hub.AI_HUB_BE.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "사용자 관리 (관리자)", description = "사용자 권한 수정, 전체 리스트 조회, 지갑 잔액 수정")
@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final SecurityContextHelper securityContextHelper;

    @Operation(summary = "사용자 권한 수정")
    @PatchMapping("/authority")
    public ResponseEntity<ApiResponse<Void>> modifyUserAuthority(
            @Valid @RequestBody ModifyUserAuthorityRequest request) {
        log.info("사용자 권한 수정 API 호출: targetUserId={}, newRole={}",
                request.userId(), request.role());

        Integer currentUserId = securityContextHelper.getCurrentUserId();
        adminUserService.modifyUserAuthority(request.userId(), request.role(), currentUserId);

        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.ok());
    }

    @Operation(summary = "전체 사용자 정보 조회")
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserListResponse>>> getAllUsers() {
        log.info("전체 사용자 정보 조회 API 호출");

        List<UserListResponse> users = adminUserService.getAllUsersWithWallet();

        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.ok(users));
    }

    @Operation(summary = "사용자 프로모션 코인 수정")
    @PatchMapping("/wallet")
    public ResponseEntity<ApiResponse<Void>> modifyUserWallet(
            @Valid @RequestBody ModifyUserWalletRequest request) {
        log.info("프로모션 코인 수정 API 호출: userId={}, promotionBalance={}",
                request.userId(), request.promotionBalance());

        Integer currentAdminId = securityContextHelper.getCurrentUserId();
        adminUserService.modifyPromotionBalance(
                request.userId(),
                request.promotionBalance(),
                currentAdminId
        );

        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.ok());
    }
}
