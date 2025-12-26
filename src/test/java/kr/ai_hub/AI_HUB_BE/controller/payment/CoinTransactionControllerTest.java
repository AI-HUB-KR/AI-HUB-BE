package kr.ai_hub.AI_HUB_BE.controller.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.ai_hub.AI_HUB_BE.application.payment.CoinTransactionService;
import kr.ai_hub.AI_HUB_BE.application.payment.dto.CoinTransactionResponse;
import kr.ai_hub.AI_HUB_BE.global.auth.SecurityContextHelper;
import kr.ai_hub.AI_HUB_BE.global.auth.jwt.JwtAuthenticationFilter;
import kr.ai_hub.AI_HUB_BE.global.auth.jwt.JwtTokenProvider;
import kr.ai_hub.AI_HUB_BE.global.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CoinTransactionController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientWebSecurityAutoConfiguration.class
}, excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityContextHelper.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class)
})
class CoinTransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CoinTransactionService coinTransactionService;

    @MockitoBean
    private SecurityContextHelper securityContextHelper;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("코인 거래 내역 조회 - 필터 없음")
    void getTransactions_NoFilter() throws Exception {
        // given
        CoinTransactionResponse transaction = CoinTransactionResponse.builder()
                .transactionId(1L)
                .coinUsage(BigDecimal.valueOf(100))
                .balanceAfter(BigDecimal.valueOf(100))
                .transactionType("CHARGE")
                .description("Test")
                .createdAt(Instant.now())
                .build();
        Page<CoinTransactionResponse> transactionPage = new PageImpl<>(List.of(transaction));

        given(coinTransactionService.getTransactions(eq(null), eq(null), eq(null), any(Pageable.class)))
                .willReturn(transactionPage);

        // when & then
        mockMvc.perform(get("/api/v1/transactions")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.detail.content").isArray())
                .andExpect(jsonPath("$.detail.content[0].transactionId").value(1))
                .andExpect(jsonPath("$.detail.content[0].transactionType").value("CHARGE"));
    }

    @Test
    @DisplayName("코인 거래 내역 조회 - 거래 타입 필터")
    void getTransactions_TypeFilter() throws Exception {
        // given
        CoinTransactionResponse transaction = CoinTransactionResponse.builder()
                .transactionId(1L)
                .coinUsage(BigDecimal.valueOf(-50))
                .balanceAfter(BigDecimal.valueOf(50))
                .transactionType("USAGE")
                .description("Test")
                .createdAt(Instant.now())
                .build();
        Page<CoinTransactionResponse> transactionPage = new PageImpl<>(List.of(transaction));

        given(coinTransactionService.getTransactions(eq("USAGE"), eq(null), eq(null), any(Pageable.class)))
                .willReturn(transactionPage);

        // when & then
        mockMvc.perform(get("/api/v1/transactions")
                .param("transactionType", "USAGE")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.detail.content[0].transactionType").value("USAGE"));
    }

    @Test
    @DisplayName("코인 거래 내역 조회 - 날짜 범위 필터")
    void getTransactions_DateFilter() throws Exception {
        // given
        CoinTransactionResponse transaction = CoinTransactionResponse.builder()
                .transactionId(1L)
                .coinUsage(BigDecimal.valueOf(100))
                .balanceAfter(BigDecimal.valueOf(100))
                .transactionType("CHARGE")
                .description("Test")
                .createdAt(Instant.now())
                .build();
        Page<CoinTransactionResponse> transactionPage = new PageImpl<>(List.of(transaction));

        given(coinTransactionService.getTransactions(eq(null), any(), any(), any(Pageable.class)))
                .willReturn(transactionPage);

        // when & then
        mockMvc.perform(get("/api/v1/transactions")
                .param("startDate", "2024-01-01")
                .param("endDate", "2024-12-31")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.detail.content").isArray());
    }

    @Test
    @DisplayName("코인 거래 내역 조회 - 타입 + 날짜 범위 필터")
    void getTransactions_TypeAndDateFilter() throws Exception {
        // given
        CoinTransactionResponse transaction = CoinTransactionResponse.builder()
                .transactionId(1L)
                .coinUsage(BigDecimal.valueOf(-50))
                .balanceAfter(BigDecimal.valueOf(50))
                .transactionType("USAGE")
                .description("Test")
                .createdAt(Instant.now())
                .build();
        Page<CoinTransactionResponse> transactionPage = new PageImpl<>(List.of(transaction));

        given(coinTransactionService.getTransactions(eq("USAGE"), any(), any(), any(Pageable.class)))
                .willReturn(transactionPage);

        // when & then
        mockMvc.perform(get("/api/v1/transactions")
                .param("transactionType", "USAGE")
                .param("startDate", "2024-01-01")
                .param("endDate", "2024-12-31")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.detail.content[0].transactionType").value("USAGE"));
    }
}
