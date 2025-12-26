package kr.ai_hub.AI_HUB_BE.aimodel.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.ai_hub.AI_HUB_BE.aimodel.service.AIModelService;
import kr.ai_hub.AI_HUB_BE.aimodel.dto.AIModelResponse;
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

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AIModelController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientWebSecurityAutoConfiguration.class
}, excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityContextHelper.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class)
})
class AIModelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AIModelService aiModelService;

    @MockitoBean
    private SecurityContextHelper securityContextHelper;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("활성화된 AI 모델 목록 조회")
    void getActiveModels() throws Exception {
        // given
        AIModelResponse model1 = AIModelResponse.builder()
                .modelId(1)
                .modelName("gpt-4")
                .displayName("GPT-4")
                .displayExplain("Advanced AI model")
                .inputPricePer1m(BigDecimal.valueOf(0.03))
                .outputPricePer1m(BigDecimal.valueOf(0.06))
                .isActive(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        AIModelResponse model2 = AIModelResponse.builder()
                .modelId(2)
                .modelName("gpt-3.5-turbo")
                .displayName("GPT-3.5 Turbo")
                .displayExplain("Fast and efficient")
                .inputPricePer1m(BigDecimal.valueOf(0.001))
                .outputPricePer1m(BigDecimal.valueOf(0.002))
                .isActive(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        given(aiModelService.getActiveModels()).willReturn(List.of(model1, model2));

        // when & then
        mockMvc.perform(get("/api/v1/models"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.detail").isArray())
                .andExpect(jsonPath("$.detail[0].modelId").value(1))
                .andExpect(jsonPath("$.detail[0].modelName").value("gpt-4"))
                .andExpect(jsonPath("$.detail[0].displayName").value("GPT-4"))
                .andExpect(jsonPath("$.detail[1].modelId").value(2))
                .andExpect(jsonPath("$.detail[1].modelName").value("gpt-3.5-turbo"));
    }

    @Test
    @DisplayName("활성화된 AI 모델 목록 조회 - 빈 목록")
    void getActiveModels_Empty() throws Exception {
        // given
        given(aiModelService.getActiveModels()).willReturn(List.of());

        // when & then
        mockMvc.perform(get("/api/v1/models"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.detail").isArray())
                .andExpect(jsonPath("$.detail").isEmpty());
    }
}
