package kr.ai_hub.AI_HUB_BE.controller.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.ai_hub.AI_HUB_BE.application.user.UserService;
import kr.ai_hub.AI_HUB_BE.application.user.dto.UpdateUserRequest;
import kr.ai_hub.AI_HUB_BE.application.user.dto.UserResponse;
import kr.ai_hub.AI_HUB_BE.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "사용자 관리", description = "사용자 정보 조회 및 수정")
@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 내 정보 조회
     * GET /api/v1/users/me
     */
    @Operation(summary = "내 정보 조회")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        log.info("내 정보 조회 API 호출");
        UserResponse response = userService.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * 내 정보 수정
     * PUT /api/v1/users/me
     */
    @Operation(summary = "내 정보 수정")
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateCurrentUser(
            @Valid @RequestBody UpdateUserRequest request
    ) {
        log.info("내 정보 수정 API 호출: username={}, email={}", request.username(), request.email());
        UserResponse response = userService.updateCurrentUser(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * 회원 탈퇴
     * DELETE /api/v1/users/me
     */
    @Operation(summary = "회원 탈퇴")
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteCurrentUser() {
        log.info("회원 탈퇴 API 호출");
        userService.deleteCurrentUser();
        return ResponseEntity.noContent().build();
    }
}
