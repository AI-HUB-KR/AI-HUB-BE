package kr.ai_hub.AI_HUB_BE.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.ai_hub.AI_HUB_BE.application.admin.AdminAIModelService;
import kr.ai_hub.AI_HUB_BE.application.admin.dto.CreateAIModelRequest;
import kr.ai_hub.AI_HUB_BE.application.admin.dto.UpdateAIModelRequest;
import kr.ai_hub.AI_HUB_BE.application.aimodel.dto.AIModelResponse;
import kr.ai_hub.AI_HUB_BE.global.auth.SecurityContextHelper;
import kr.ai_hub.AI_HUB_BE.global.auth.jwt.JwtAuthenticationFilter;
import kr.ai_hub.AI_HUB_BE.global.auth.jwt.JwtTokenProvider;
import kr.ai_hub.AI_HUB_BE.global.config.SecurityConfig;
import kr.ai_hub.AI_HUB_BE.global.error.exception.ModelNotFoundException;
import kr.ai_hub.AI_HUB_BE.global.error.exception.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminAIModelController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientWebSecurityAutoConfiguration.class
}, excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityContextHelper.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class)
})
class AdminAIModelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminAIModelService adminAIModelService;

    @MockitoBean
    private SecurityContextHelper securityContextHelper;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("AI 모델 생성 - 성공")
    void createModel_Success() throws Exception {
        // given
        CreateAIModelRequest request = new CreateAIModelRequest(
                "gpt-4",
                "GPT-4",
                "Advanced AI model",
                BigDecimal.valueOf(0.03),
                BigDecimal.valueOf(0.06),
                true);

        AIModelResponse response = AIModelResponse.builder()
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

        given(adminAIModelService.createModel(any(CreateAIModelRequest.class)))
                .willReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/admin/models")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.detail.modelName").value("gpt-4"))
                .andExpect(jsonPath("$.detail.displayName").value("GPT-4"));
    }

    @Test
    @DisplayName("AI 모델 생성 - 중복 모델명")
    void createModel_DuplicateModelName() throws Exception {
        // given
        CreateAIModelRequest request = new CreateAIModelRequest(
                "gpt-4",
                "GPT-4",
                "Advanced AI model",
                BigDecimal.valueOf(0.03),
                BigDecimal.valueOf(0.06),
                true);

        given(adminAIModelService.createModel(any(CreateAIModelRequest.class)))
                .willThrow(new ValidationException("이미 존재하는 모델 이름입니다"));

        // when & then
        mockMvc.perform(post("/api/v1/admin/models")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("AI 모델 수정 - 성공")
    void updateModel_Success() throws Exception {
        // given
        Integer modelId = 1;
        UpdateAIModelRequest request = new UpdateAIModelRequest(
                "GPT-4 Updated",
                "Updated description",
                BigDecimal.valueOf(0.04),
                BigDecimal.valueOf(0.08),
                false);

        AIModelResponse response = AIModelResponse.builder()
                .modelId(modelId)
                .modelName("gpt-4")
                .displayName("GPT-4 Updated")
                .displayExplain("Updated description")
                .inputPricePer1m(BigDecimal.valueOf(0.04))
                .outputPricePer1m(BigDecimal.valueOf(0.08))
                .isActive(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        given(adminAIModelService.updateModel(eq(modelId), any(UpdateAIModelRequest.class)))
                .willReturn(response);

        // when & then
        mockMvc.perform(put("/api/v1/admin/models/{modelId}", modelId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.detail.displayName").value("GPT-4 Updated"))
                .andExpect(jsonPath("$.detail.isActive").value(false));
    }

    @Test
    @DisplayName("AI 모델 수정 - 모델 없음")
    void updateModel_ModelNotFound() throws Exception {
        // given
        Integer modelId = 999;
        UpdateAIModelRequest request = new UpdateAIModelRequest(
                "GPT-4 Updated",
                null,
                null,
                null,
                null);

        given(adminAIModelService.updateModel(eq(modelId), any(UpdateAIModelRequest.class)))
                .willThrow(new ModelNotFoundException("모델을 찾을 수 없습니다"));

        // when & then
        mockMvc.perform(put("/api/v1/admin/models/{modelId}", modelId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("AI 모델 삭제 - 성공")
    void deleteModel_Success() throws Exception {
        // given
        Integer modelId = 1;

        // when & then
        mockMvc.perform(delete("/api/v1/admin/models/{modelId}", modelId))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("AI 모델 삭제 - 모델 없음")
    void deleteModel_ModelNotFound() throws Exception {
        // given
        Integer modelId = 999;

        doThrow(new ModelNotFoundException("모델을 찾을 수 없습니다"))
                .when(adminAIModelService).deleteModel(modelId);

        // when & then
        mockMvc.perform(delete("/api/v1/admin/models/{modelId}", modelId))
                .andExpect(status().isNotFound());
    }
}
