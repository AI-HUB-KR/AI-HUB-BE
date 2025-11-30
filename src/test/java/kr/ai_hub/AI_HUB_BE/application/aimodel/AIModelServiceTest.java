package kr.ai_hub.AI_HUB_BE.application.aimodel;

import kr.ai_hub.AI_HUB_BE.application.aimodel.dto.AIModelResponse;
import kr.ai_hub.AI_HUB_BE.domain.aimodel.AIModel;
import kr.ai_hub.AI_HUB_BE.domain.aimodel.AIModelRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AIModelServiceTest {

    @InjectMocks
    private AIModelService aiModelService;

    @Mock
    private AIModelRepository aiModelRepository;

    @Test
    @DisplayName("활성화된 AI 모델 목록 조회")
    void getActiveModels() {
        // given
        AIModel model1 = AIModel.builder()
                .modelId(1)
                .modelName("gpt-4")
                .displayName("GPT-4")
                .displayExplain("Advanced AI model")
                .inputPricePer1m(BigDecimal.valueOf(0.03))
                .outputPricePer1m(BigDecimal.valueOf(0.06))
                .isActive(true)
                .build();

        // Set createdAt using reflection
        try {
            java.lang.reflect.Field createdAtField = AIModel.class.getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(model1, LocalDateTime.now());

            java.lang.reflect.Field updatedAtField = AIModel.class.getDeclaredField("updatedAt");
            updatedAtField.setAccessible(true);
            updatedAtField.set(model1, LocalDateTime.now());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        AIModel model2 = AIModel.builder()
                .modelId(2)
                .modelName("gpt-3.5-turbo")
                .displayName("GPT-3.5 Turbo")
                .displayExplain("Fast and efficient")
                .inputPricePer1m(BigDecimal.valueOf(0.001))
                .outputPricePer1m(BigDecimal.valueOf(0.002))
                .isActive(true)
                .build();

        try {
            java.lang.reflect.Field createdAtField = AIModel.class.getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(model2, LocalDateTime.now());

            java.lang.reflect.Field updatedAtField = AIModel.class.getDeclaredField("updatedAt");
            updatedAtField.setAccessible(true);
            updatedAtField.set(model2, LocalDateTime.now());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        given(aiModelRepository.findByIsActiveTrueOrderByCreatedAtDesc())
                .willReturn(List.of(model1, model2));

        // when
        List<AIModelResponse> result = aiModelService.getActiveModels();

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).modelName()).isEqualTo("gpt-4");
        assertThat(result.get(1).modelName()).isEqualTo("gpt-3.5-turbo");
    }

    @Test
    @DisplayName("활성화된 AI 모델 목록 조회 - 빈 목록")
    void getActiveModels_Empty() {
        // given
        given(aiModelRepository.findByIsActiveTrueOrderByCreatedAtDesc())
                .willReturn(List.of());

        // when
        List<AIModelResponse> result = aiModelService.getActiveModels();

        // then
        assertThat(result).isEmpty();
    }
}
