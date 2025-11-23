package kr.ai_hub.AI_HUB_BE.controller.dashboard;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import kr.ai_hub.AI_HUB_BE.application.dashboard.DashboardService;
import kr.ai_hub.AI_HUB_BE.application.dashboard.dto.ModelPricingResponse;
import kr.ai_hub.AI_HUB_BE.application.dashboard.dto.MonthlyUsageResponse;
import kr.ai_hub.AI_HUB_BE.application.dashboard.dto.UserStatsResponse;
import kr.ai_hub.AI_HUB_BE.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Validated
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * 모든 활성화된 AI 모델의 가격 정보를 조회합니다 (Public API).
     */
    @GetMapping("/models/pricing")
    public ResponseEntity<ApiResponse<List<ModelPricingResponse>>> getModelPricing() {
        log.info("모델 가격 대시보드 API 호출");

        List<ModelPricingResponse> response = dashboardService.getModelPricing();

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * 현재 사용자의 월별 모델별 코인 사용량 통계를 조회합니다.
     */
    @GetMapping("/usage/monthly")
    public ResponseEntity<ApiResponse<MonthlyUsageResponse>> getMonthlyUsage(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) @Min(1) @Max(12) Integer month) {
        log.info("월별 사용량 대시보드 API 호출: year={}, month={}", year, month);

        // 기본값: 현재 연도와 월
        LocalDate now = LocalDate.now();
        int targetYear = year != null ? year : now.getYear();
        int targetMonth = month != null ? month : now.getMonthValue();

        MonthlyUsageResponse response = dashboardService.getMonthlyUsage(targetYear, targetMonth);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * 현재 사용자의 코인 및 활동 통계를 요약합니다.
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<UserStatsResponse>> getUserStats() {
        log.info("사용자 통계 요약 API 호출");

        UserStatsResponse response = dashboardService.getUserStats();

        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
