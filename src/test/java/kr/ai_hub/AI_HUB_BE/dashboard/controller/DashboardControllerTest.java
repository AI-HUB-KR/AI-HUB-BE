package kr.ai_hub.AI_HUB_BE.dashboard.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.ai_hub.AI_HUB_BE.dashboard.service.DashboardService;
import kr.ai_hub.AI_HUB_BE.dashboard.dto.ModelPricingResponse;
import kr.ai_hub.AI_HUB_BE.dashboard.dto.MonthlyUsageResponse;
import kr.ai_hub.AI_HUB_BE.dashboard.dto.UserStatsResponse;
import kr.ai_hub.AI_HUB_BE.global.security.SecurityContextHelper;
import kr.ai_hub.AI_HUB_BE.global.security.jwt.JwtAuthenticationFilter;
import kr.ai_hub.AI_HUB_BE.global.security.jwt.JwtTokenProvider;
import kr.ai_hub.AI_HUB_BE.global.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DashboardController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientWebSecurityAutoConfiguration.class
}, excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityContextHelper.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class)
})
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    @MockitoBean
    private SecurityContextHelper securityContextHelper;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("모델 가격 조회 - 성공")
    void getModelPricing_Success() throws Exception {
        // given
        ModelPricingResponse pricing = ModelPricingResponse.builder()
                .modelId(1)
                .modelName("gpt-4")
                .displayName("GPT-4")
                .inputPricePer1m(BigDecimal.valueOf(0.03))
                .outputPricePer1m(BigDecimal.valueOf(0.06))
                .build();

        given(dashboardService.getModelPricing()).willReturn(List.of(pricing));

        // when & then
        mockMvc.perform(get("/api/v1/dashboard/models/pricing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.detail").isArray())
                .andExpect(jsonPath("$.detail[0].modelName").value("gpt-4"));
    }

    @Test
    @DisplayName("월별 사용량 조회 - 성공")
    void getMonthlyUsage_Success() throws Exception {
        // given
        MonthlyUsageResponse usage = MonthlyUsageResponse.builder()
                .year(2024)
                .month(1)
                .totalCoinUsed(BigDecimal.valueOf(100))
                .modelUsage(List.of())
                .dailyUsage(List.of())
                .build();

        given(dashboardService.getMonthlyUsage(anyInt(), anyInt())).willReturn(usage);

        // when & then
        mockMvc.perform(get("/api/v1/dashboard/usage/monthly")
                .param("year", "2024")
                .param("month", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.detail.year").value(2024))
                .andExpect(jsonPath("$.detail.month").value(1));
    }

    @Test
    @DisplayName("사용자 통계 조회 - 성공")
    void getUserStats_Success() throws Exception {
        // given
        UserStatsResponse stats = UserStatsResponse.builder()
                .totalCoinPurchased(BigDecimal.valueOf(1000))
                .totalCoinUsed(BigDecimal.valueOf(500))
                .currentBalance(BigDecimal.valueOf(500))
                .totalMessages(100L)
                .totalChatRooms(10L)
                .mostUsedModel(null)
                .last30DaysUsage(BigDecimal.valueOf(50))
                .memberSince(Instant.now())
                .build();

        given(dashboardService.getUserStats()).willReturn(stats);

        // when & then
        mockMvc.perform(get("/api/v1/dashboard/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.detail.totalMessages").value(100))
                .andExpect(jsonPath("$.detail.totalChatRooms").value(10));
    }
}
