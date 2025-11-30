package kr.ai_hub.AI_HUB_BE.controller.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.ai_hub.AI_HUB_BE.application.payment.PaymentHistoryService;
import kr.ai_hub.AI_HUB_BE.application.payment.dto.PaymentResponse;
import kr.ai_hub.AI_HUB_BE.global.auth.SecurityContextHelper;
import kr.ai_hub.AI_HUB_BE.global.config.SecurityConfig;
import kr.ai_hub.AI_HUB_BE.global.error.exception.ForbiddenException;
import kr.ai_hub.AI_HUB_BE.global.error.exception.PaymentNotFoundException;
import kr.ai_hub.AI_HUB_BE.global.auth.jwt.JwtAuthenticationFilter;
import kr.ai_hub.AI_HUB_BE.global.auth.jwt.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PaymentHistoryController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientWebSecurityAutoConfiguration.class
}, excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityContextHelper.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class)
})
class PaymentHistoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentHistoryService paymentHistoryService;

    @MockitoBean
    private SecurityContextHelper securityContextHelper;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("결제 내역 목록 조회 - 성공")
    void getPayments_Success() throws Exception {
        // given
        PaymentResponse payment = new PaymentResponse(
                1L,
                "tx_123",
                "CARD",
                BigDecimal.valueOf(10000),
                null,
                BigDecimal.valueOf(100),
                BigDecimal.ZERO,
                "COMPLETED",
                null,
                null,
                Instant.now(),
                null);
        Page<PaymentResponse> paymentPage = new PageImpl<>(List.of(payment));

        given(paymentHistoryService.getPayments(eq(null), any(Pageable.class)))
                .willReturn(paymentPage);

        // when & then
        mockMvc.perform(get("/api/v1/payments")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.detail.content").isArray())
                .andExpect(jsonPath("$.detail.content[0].paymentId").value(1))
                .andExpect(jsonPath("$.detail.content[0].status").value("COMPLETED"));
    }

    @Test
    @DisplayName("결제 내역 목록 조회 - 상태 필터링")
    void getPayments_WithStatusFilter() throws Exception {
        // given
        PaymentResponse payment = new PaymentResponse(
                1L,
                "tx_123",
                "CARD",
                BigDecimal.valueOf(10000),
                null,
                BigDecimal.valueOf(100),
                BigDecimal.ZERO,
                "COMPLETED",
                null,
                null,
                Instant.now(),
                null);
        Page<PaymentResponse> paymentPage = new PageImpl<>(List.of(payment));

        given(paymentHistoryService.getPayments(eq("COMPLETED"), any(Pageable.class)))
                .willReturn(paymentPage);

        // when & then
        mockMvc.perform(get("/api/v1/payments")
                .param("status", "COMPLETED")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.detail.content").isArray())
                .andExpect(jsonPath("$.detail.content[0].status").value("COMPLETED"));
    }

    @Test
    @DisplayName("결제 상세 조회 - 성공")
    void getPayment_Success() throws Exception {
        // given
        Long paymentId = 1L;
        PaymentResponse payment = new PaymentResponse(
                paymentId,
                "tx_123",
                "CARD",
                BigDecimal.valueOf(10000),
                null,
                BigDecimal.valueOf(100),
                BigDecimal.ZERO,
                "COMPLETED",
                null,
                null,
                Instant.now(),
                null);
        given(paymentHistoryService.getPayment(paymentId))
                .willReturn(payment);

        // when & then
        mockMvc.perform(get("/api/v1/payments/{paymentId}", paymentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.detail.paymentId").value(1))
                .andExpect(jsonPath("$.detail.transactionId").value("tx_123"))
                .andExpect(jsonPath("$.detail.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("결제 상세 조회 - 결제 내역 없음")
    void getPayment_NotFound() throws Exception {
        // given
        Long paymentId = 999L;

        given(paymentHistoryService.getPayment(paymentId))
                .willThrow(new PaymentNotFoundException("결제 내역을 찾을 수 없습니다: " + paymentId));

        // when & then
        mockMvc.perform(get("/api/v1/payments/{paymentId}", paymentId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("결제 상세 조회 - 권한 없음")
    void getPayment_Forbidden() throws Exception {
        // given
        Long paymentId = 1L;

        given(paymentHistoryService.getPayment(paymentId))
                .willThrow(new ForbiddenException("해당 결제 내역에 접근할 권한이 없습니다"));

        // when & then
        mockMvc.perform(get("/api/v1/payments/{paymentId}", paymentId))
                .andExpect(status().isForbidden());
    }
}
