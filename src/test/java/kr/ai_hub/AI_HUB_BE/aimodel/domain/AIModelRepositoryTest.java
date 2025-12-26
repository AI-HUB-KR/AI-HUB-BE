package kr.ai_hub.AI_HUB_BE.aimodel.domain;

import kr.ai_hub.AI_HUB_BE.global.config.JpaAuditingConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
class AIModelRepositoryTest {

    @Autowired
    private AIModelRepository aiModelRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("모델명으로 AI 모델 조회")
    void findByModelName() {
        // given
        AIModel model = AIModel.builder()
                .modelName("gpt-4")
                .displayName("GPT-4")
                .displayExplain("Advanced AI model")
                .inputPricePer1m(BigDecimal.valueOf(0.03))
                .outputPricePer1m(BigDecimal.valueOf(0.06))
                .isActive(true)
                .build();
        entityManager.persistAndFlush(model);

        // when
        Optional<AIModel> result = aiModelRepository.findByModelName("gpt-4");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getModelName()).isEqualTo("gpt-4");
        assertThat(result.get().getDisplayName()).isEqualTo("GPT-4");
    }

    @Test
    @DisplayName("모델명으로 AI 모델 조회 - 존재하지 않음")
    void findByModelName_NotFound() {
        // when
        Optional<AIModel> result = aiModelRepository.findByModelName("non-existent-model");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("활성 상태로 AI 모델 조회")
    void findByIsActive() {
        // given
        AIModel activeModel1 = AIModel.builder()
                .modelName("gpt-4")
                .displayName("GPT-4")
                .inputPricePer1m(BigDecimal.valueOf(0.03))
                .outputPricePer1m(BigDecimal.valueOf(0.06))
                .isActive(true)
                .build();

        AIModel activeModel2 = AIModel.builder()
                .modelName("gpt-3.5-turbo")
                .displayName("GPT-3.5 Turbo")
                .inputPricePer1m(BigDecimal.valueOf(0.001))
                .outputPricePer1m(BigDecimal.valueOf(0.002))
                .isActive(true)
                .build();

        AIModel inactiveModel = AIModel.builder()
                .modelName("gpt-3")
                .displayName("GPT-3")
                .inputPricePer1m(BigDecimal.valueOf(0.02))
                .outputPricePer1m(BigDecimal.valueOf(0.02))
                .isActive(false)
                .build();

        entityManager.persist(activeModel1);
        entityManager.persist(activeModel2);
        entityManager.persist(inactiveModel);
        entityManager.flush();

        // when
        List<AIModel> activeModels = aiModelRepository.findByIsActive(true);
        List<AIModel> inactiveModels = aiModelRepository.findByIsActive(false);

        // then
        assertThat(activeModels).hasSize(2);
        assertThat(activeModels).extracting(AIModel::getModelName)
                .containsExactlyInAnyOrder("gpt-4", "gpt-3.5-turbo");

        assertThat(inactiveModels).hasSize(1);
        assertThat(inactiveModels.get(0).getModelName()).isEqualTo("gpt-3");
    }

    @Test
    @DisplayName("활성 AI 모델 생성일 역순 조회")
    void findByIsActiveTrueOrderByCreatedAtDesc() throws InterruptedException {
        // given
        AIModel model1 = AIModel.builder()
                .modelName("gpt-3.5-turbo")
                .displayName("GPT-3.5 Turbo")
                .inputPricePer1m(BigDecimal.valueOf(0.001))
                .outputPricePer1m(BigDecimal.valueOf(0.002))
                .isActive(true)
                .build();
        entityManager.persistAndFlush(model1);

        Thread.sleep(10); // Ensure different timestamps

        AIModel model2 = AIModel.builder()
                .modelName("gpt-4")
                .displayName("GPT-4")
                .inputPricePer1m(BigDecimal.valueOf(0.03))
                .outputPricePer1m(BigDecimal.valueOf(0.06))
                .isActive(true)
                .build();
        entityManager.persistAndFlush(model2);

        AIModel inactiveModel = AIModel.builder()
                .modelName("gpt-3")
                .displayName("GPT-3")
                .inputPricePer1m(BigDecimal.valueOf(0.02))
                .outputPricePer1m(BigDecimal.valueOf(0.02))
                .isActive(false)
                .build();
        entityManager.persistAndFlush(inactiveModel);

        // when
        List<AIModel> result = aiModelRepository.findByIsActiveTrueOrderByCreatedAtDesc();

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getModelName()).isEqualTo("gpt-4"); // Most recent
        assertThat(result.get(1).getModelName()).isEqualTo("gpt-3.5-turbo");
    }
}
