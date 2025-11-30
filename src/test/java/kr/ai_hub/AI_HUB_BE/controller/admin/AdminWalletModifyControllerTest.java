package kr.ai_hub.AI_HUB_BE.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.ai_hub.AI_HUB_BE.application.admin.AdminWalletModifyService;
import kr.ai_hub.AI_HUB_BE.global.auth.SecurityContextHelper;
import kr.ai_hub.AI_HUB_BE.global.auth.jwt.JwtAuthenticationFilter;
import kr.ai_hub.AI_HUB_BE.global.auth.jwt.JwtTokenProvider;
import kr.ai_hub.AI_HUB_BE.global.config.SecurityConfig;
import kr.ai_hub.AI_HUB_BE.global.error.exception.WalletNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminWalletModifyController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientWebSecurityAutoConfiguration.class
}, excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityContextHelper.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class)
})
class AdminWalletModifyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminWalletModifyService adminWalletModifyService;

    @MockBean
    private SecurityContextHelper securityContextHelper;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("사용자 지갑 잔액 수정 - 성공")
    void modifyUserWallet_Success() throws Exception {
        // given
        Long userId = 1L;
        Integer amount = 1000;

        // when & then
        mockMvc.perform(patch("/api/v1/admin/wallet")
                .param("userId", userId.toString())
                .param("amount", amount.toString()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("사용자 지갑 잔액 수정 - 지갑 없음")
    void modifyUserWallet_WalletNotFound() throws Exception {
        // given
        Long userId = 999L;
        Integer amount = 1000;

        doThrow(new WalletNotFoundException("지갑을 찾을 수 없습니다"))
                .when(adminWalletModifyService).setUserBalance(eq(userId.intValue()), any(BigDecimal.class));

        // when & then
        mockMvc.perform(patch("/api/v1/admin/wallet")
                .param("userId", userId.toString())
                .param("amount", amount.toString()))
                .andExpect(status().isNotFound());
    }
}
