package kr.ai_hub.AI_HUB_BE.admin.service;

import kr.ai_hub.AI_HUB_BE.admin.dto.CreateAIModelRequest;
import kr.ai_hub.AI_HUB_BE.admin.dto.UpdateAIModelRequest;
import kr.ai_hub.AI_HUB_BE.aimodel.dto.AIModelDetailResponse;
import kr.ai_hub.AI_HUB_BE.aimodel.dto.AIModelResponse;
import kr.ai_hub.AI_HUB_BE.aimodel.domain.AIModel;
import kr.ai_hub.AI_HUB_BE.aimodel.domain.AIModelRepository;
import kr.ai_hub.AI_HUB_BE.user.domain.UserRepository;
import kr.ai_hub.AI_HUB_BE.global.security.SecurityContextHelper;
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
                BigDecimal.ZERO,
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
                BigDecimal.ZERO,
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
                BigDecimal.ZERO,
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
                null,
                null);

        given(aiModelRepository.findById(modelId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminAIModelService.updateModel(modelId, request))
                .isInstanceOf(ModelNotFoundException.class)
                .hasMessageContaining("모델을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("AI 모델 상세 조회 - 성공")
    void getModelDetails_Success() {
        // given
        Integer modelId = 1;
        BigDecimal markupRate = BigDecimal.valueOf(0.2);
        BigDecimal inputCost = BigDecimal.valueOf(0.02);
        BigDecimal outputCost = BigDecimal.valueOf(0.04);

        AIModel model = AIModel.builder()
                .modelId(modelId)
                .modelName("gpt-4")
                .displayName("GPT-4")
                .displayExplain("Advanced AI model")
                .inputPricePer1m(inputCost)
                .outputPricePer1m(outputCost)
                .modelMarkupRate(markupRate)
                .isActive(true)
                .build();

        try {
            java.lang.reflect.Field createdAtField = AIModel.class.getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(model, LocalDateTime.now());

            java.lang.reflect.Field updatedAtField = AIModel.class.getDeclaredField("updatedAt");
            updatedAtField.setAccessible(true);
            updatedAtField.set(model, LocalDateTime.now());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        given(aiModelRepository.findById(modelId)).willReturn(Optional.of(model));

        // when
        AIModelDetailResponse result = adminAIModelService.getModelDetails(modelId);

        // then
        assertThat(result.modelId()).isEqualTo(modelId);
        assertThat(result.inputPricePer1m()).isEqualByComparingTo(inputCost);
        assertThat(result.outputPricePer1m()).isEqualByComparingTo(outputCost);
        assertThat(result.modelMarkupRate()).isEqualByComparingTo(markupRate);
    }

    @Test
    @DisplayName("AI 모델 상세 조회 - 모델 없음")
    void getModelDetails_ModelNotFound() {
        // given
        Integer modelId = 999;

        given(aiModelRepository.findById(modelId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminAIModelService.getModelDetails(modelId))
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
