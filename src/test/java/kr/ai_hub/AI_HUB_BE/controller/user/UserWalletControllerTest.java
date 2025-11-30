package kr.ai_hub.AI_HUB_BE.controller.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import kr.ai_hub.AI_HUB_BE.application.userwallet.UserWalletService;
import kr.ai_hub.AI_HUB_BE.application.userwallet.dto.BalanceResponse;
import kr.ai_hub.AI_HUB_BE.application.userwallet.dto.UserWalletResponse;
import kr.ai_hub.AI_HUB_BE.global.auth.SecurityContextHelper;
import kr.ai_hub.AI_HUB_BE.global.error.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserWalletControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserWalletService userWalletService;

    @Mock
    private SecurityContextHelper securityContextHelper;

    @InjectMocks
    private UserWalletController userWalletController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockMvc = MockMvcBuilders.standaloneSetup(userWalletController)
                .setControllerAdvice(new GlobalExceptionHandler(securityContextHelper))
                .build();
    }

    @Test
    @DisplayName("지갑 상세 조회")
    void getUserWallet() throws Exception {
        // given
        UserWalletResponse response = UserWalletResponse.builder()
                .walletId(1)
                .userId(1)
                .balance(BigDecimal.valueOf(100.0))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        given(userWalletService.getUserWallet()).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/wallet"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.detail.walletId").value(1))
                .andExpect(jsonPath("$.detail.userId").value(1))
                .andExpect(jsonPath("$.detail.balance").value(100.0));
    }

    @Test
    @DisplayName("코인 잔액 조회")
    void getBalance() throws Exception {
        // given
        BalanceResponse response = BalanceResponse.builder()
                .balance(BigDecimal.valueOf(50.5))
                .build();

        given(userWalletService.getBalance()).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/wallet/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.detail.balance").value(50.5));
    }
}
