package kr.ai_hub.AI_HUB_BE.application.admin;

import kr.ai_hub.AI_HUB_BE.application.admin.dto.CreateAIModelRequest;
import kr.ai_hub.AI_HUB_BE.application.admin.dto.UpdateAIModelRequest;
import kr.ai_hub.AI_HUB_BE.application.aimodel.dto.AIModelDetailResponse;
import kr.ai_hub.AI_HUB_BE.application.aimodel.dto.AIModelResponse;
import kr.ai_hub.AI_HUB_BE.domain.aimodel.AIModel;
import kr.ai_hub.AI_HUB_BE.domain.aimodel.AIModelRepository;
import kr.ai_hub.AI_HUB_BE.domain.user.User;
import kr.ai_hub.AI_HUB_BE.domain.user.UserRole;
import kr.ai_hub.AI_HUB_BE.domain.user.UserRepository;
import kr.ai_hub.AI_HUB_BE.global.auth.SecurityContextHelper;
import kr.ai_hub.AI_HUB_BE.global.error.exception.ForbiddenException;
import kr.ai_hub.AI_HUB_BE.global.error.exception.ModelNotFoundException;
import kr.ai_hub.AI_HUB_BE.global.error.exception.UserNotFoundException;
import kr.ai_hub.AI_HUB_BE.global.error.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminAIModelService {

    private final AIModelRepository aiModelRepository;

    /**
     * 새로운 AI 모델을 등록합니다 (관리자 전용).
     */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public AIModelResponse createModel(CreateAIModelRequest request) {
        log.info("관리자 모델 생성 요청: modelName={}", request.modelName());

        // 모델명 중복 체크
        if (aiModelRepository.findByModelName(request.modelName()).isPresent()) {
            throw new ValidationException("이미 존재하는 모델 이름입니다: " + request.modelName());
        }

        AIModel model = AIModel.builder()
                .modelName(request.modelName())
                .displayName(request.displayName())
                .displayExplain(request.displayExplain())
                .inputPricePer1m(request.inputPricePer1m())
                .outputPricePer1m(request.outputPricePer1m())
                .modelMarkupRate(request.modelMarkupRate())
                .isActive(request.isActive())
                .build();

        AIModel savedModel = aiModelRepository.save(model);
        log.info("모델 생성 완료: modelId={}, modelName={}", savedModel.getModelId(), savedModel.getModelName());

        return AIModelResponse.from(savedModel);
    }

    /**
     * AI 모델 토큰 원가와 markup를 포함한 상세 정보를 조회합니다 (관리자 전용).
     */
    @PreAuthorize("hasRole('ADMIN')")
    public AIModelDetailResponse getModelDetails(Integer modelId) {
        log.info("관리자 모델 상세 조회 요청: modelId={}", modelId);

        AIModel model = aiModelRepository.findById(modelId)
                .orElseThrow(() -> new ModelNotFoundException("모델을 찾을 수 없습니다: " + modelId));

        return AIModelDetailResponse.from(model);
    }

    /**
     * AI 모델 정보를 수정합니다 (관리자 전용).
     */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public AIModelResponse updateModel(Integer modelId, UpdateAIModelRequest request) {
        log.info("관리자 모델 수정 요청: modelId={}", modelId);

        AIModel model = aiModelRepository.findById(modelId)
                .orElseThrow(() -> new ModelNotFoundException("모델을 찾을 수 없습니다: " + modelId));

        model.update(
                request.displayName(),
                request.displayExplain(),
                request.inputPricePer1m(),
                request.outputPricePer1m(),
                request.modelMarkupRate(),
                request.isActive()
        );

        log.info("모델 수정 완료: modelId={}", modelId);

        return AIModelResponse.from(model);
    }

    /**
     * AI 모델을 삭제(soft-delete)합니다 (관리자 전용).
     */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteModel(Integer modelId) {
        log.info("관리자 모델 삭제(deactivate) 요청: modelId={}", modelId);

        aiModelRepository
                .findById(modelId)
                .orElseThrow(() -> new ModelNotFoundException("모델을 찾을 수 없습니다: " + modelId))
                .deactivate();

        log.info("모델 삭제(deactivate) 완료: modelId={}", modelId);
    }

}
