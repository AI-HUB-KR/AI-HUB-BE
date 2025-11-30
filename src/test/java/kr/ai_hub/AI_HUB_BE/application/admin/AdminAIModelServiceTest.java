package kr.ai_hub.AI_HUB_BE.application.admin;

import kr.ai_hub.AI_HUB_BE.application.admin.dto.CreateAIModelRequest;
import kr.ai_hub.AI_HUB_BE.application.admin.dto.UpdateAIModelRequest;
import kr.ai_hub.AI_HUB_BE.application.aimodel.dto.AIModelResponse;
import kr.ai_hub.AI_HUB_BE.domain.aimodel.AIModel;
import kr.ai_hub.AI_HUB_BE.domain.aimodel.AIModelRepository;
import kr.ai_hub.AI_HUB_BE.domain.user.UserRepository;
import kr.ai_hub.AI_HUB_BE.global.auth.SecurityContextHelper;
import kr.ai_hub.AI_HUB_BE.global.error.exception.ModelNotFoundException;
import kr.ai_hub.AI_HUB_BE.global.error.exception.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminAIModelServiceTest {

    @InjectMocks
    private AdminAIModelService adminAIModelService;

    @Mock
    private AIModelRepository aiModelRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityContextHelper securityContextHelper;

    @Test
    @DisplayName("AI 모델 생성 - 성공")
    void createModel_Success() {
        // given
        CreateAIModelRequest request = new CreateAIModelRequest(
                "gpt-4",
                "GPT-4",
                "Advanced AI model",
                BigDecimal.valueOf(0.03),
                BigDecimal.valueOf(0.06),
                true);

        AIModel savedModel = AIModel.builder()
                .modelId(1)
                .modelName("gpt-4")
                .displayName("GPT-4")
                .displayExplain("Advanced AI model")
                .inputPricePer1m(BigDecimal.valueOf(0.03))
                .outputPricePer1m(BigDecimal.valueOf(0.06))
                .isActive(true)
                .build();

        // Set timestamps using reflection
        try {
            java.lang.reflect.Field createdAtField = AIModel.class.getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(savedModel, LocalDateTime.now());

            java.lang.reflect.Field updatedAtField = AIModel.class.getDeclaredField("updatedAt");
            updatedAtField.setAccessible(true);
            updatedAtField.set(savedModel, LocalDateTime.now());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        given(aiModelRepository.findByModelName("gpt-4")).willReturn(Optional.empty());
        given(aiModelRepository.save(any(AIModel.class))).willReturn(savedModel);

        // when
        AIModelResponse result = adminAIModelService.createModel(request);

        // then
        assertThat(result.modelName()).isEqualTo("gpt-4");
        assertThat(result.displayName()).isEqualTo("GPT-4");
        verify(aiModelRepository).save(any(AIModel.class));
    }

    @Test
    @DisplayName("AI 모델 생성 - 중복 모델명")
    void createModel_DuplicateModelName() {
        // given
        CreateAIModelRequest request = new CreateAIModelRequest(
                "gpt-4",
                "GPT-4",
                "Advanced AI model",
                BigDecimal.valueOf(0.03),
                BigDecimal.valueOf(0.06),
                true);

        AIModel existingModel = AIModel.builder()
                .modelId(1)
                .modelName("gpt-4")
                .build();

        given(aiModelRepository.findByModelName("gpt-4")).willReturn(Optional.of(existingModel));

        // when & then
        assertThatThrownBy(() -> adminAIModelService.createModel(request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("이미 존재하는 모델 이름입니다");
    }

    @Test
    @DisplayName("AI 모델 수정 - 성공")
    void updateModel_Success() {
        // given
        Integer modelId = 1;
        UpdateAIModelRequest request = new UpdateAIModelRequest(
                "GPT-4 Updated",
                "Updated description",
                BigDecimal.valueOf(0.04),
                BigDecimal.valueOf(0.08),
                false);

        AIModel existingModel = AIModel.builder()
                .modelId(modelId)
                .modelName("gpt-4")
                .displayName("GPT-4")
                .displayExplain("Advanced AI model")
                .inputPricePer1m(BigDecimal.valueOf(0.03))
                .outputPricePer1m(BigDecimal.valueOf(0.06))
                .isActive(true)
                .build();

        try {
            java.lang.reflect.Field createdAtField = AIModel.class.getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(existingModel, LocalDateTime.now());

            java.lang.reflect.Field updatedAtField = AIModel.class.getDeclaredField("updatedAt");
            updatedAtField.setAccessible(true);
            updatedAtField.set(existingModel, LocalDateTime.now());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        given(aiModelRepository.findById(modelId)).willReturn(Optional.of(existingModel));

        // when
        AIModelResponse result = adminAIModelService.updateModel(modelId, request);

        // then
        assertThat(result.displayName()).isEqualTo("GPT-4 Updated");
        assertThat(result.isActive()).isFalse();
    }

    @Test
    @DisplayName("AI 모델 수정 - 모델 없음")
    void updateModel_ModelNotFound() {
        // given
        Integer modelId = 999;
        UpdateAIModelRequest request = new UpdateAIModelRequest(
                "GPT-4 Updated",
                null,
                null,
                null,
                null);

        given(aiModelRepository.findById(modelId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminAIModelService.updateModel(modelId, request))
                .isInstanceOf(ModelNotFoundException.class)
                .hasMessageContaining("모델을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("AI 모델 삭제(비활성화) - 성공")
    void deleteModel_Success() {
        // given
        Integer modelId = 1;
        AIModel existingModel = AIModel.builder()
                .modelId(modelId)
                .modelName("gpt-4")
                .displayName("GPT-4")
                .inputPricePer1m(BigDecimal.valueOf(0.03))
                .outputPricePer1m(BigDecimal.valueOf(0.06))
                .isActive(true)
                .build();

        given(aiModelRepository.findById(modelId)).willReturn(Optional.of(existingModel));

        // when
        adminAIModelService.deleteModel(modelId);

        // then
        assertThat(existingModel.getIsActive()).isFalse();
    }

    @Test
    @DisplayName("AI 모델 삭제 - 모델 없음")
    void deleteModel_ModelNotFound() {
        // given
        Integer modelId = 999;

        given(aiModelRepository.findById(modelId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminAIModelService.deleteModel(modelId))
                .isInstanceOf(ModelNotFoundException.class)
                .hasMessageContaining("모델을 찾을 수 없습니다");
    }
}
