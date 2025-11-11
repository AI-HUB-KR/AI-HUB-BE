package kr.ai_hub.AI_HUB_BE.application.admin.aimodel;

import kr.ai_hub.AI_HUB_BE.application.admin.aimodel.dto.CreateAIModelRequest;
import kr.ai_hub.AI_HUB_BE.application.admin.aimodel.dto.UpdateAIModelRequest;
import kr.ai_hub.AI_HUB_BE.application.aimodel.dto.AIModelResponse;
import kr.ai_hub.AI_HUB_BE.domain.aimodel.entity.AIModel;
import kr.ai_hub.AI_HUB_BE.domain.aimodel.repository.AIModelRepository;
import kr.ai_hub.AI_HUB_BE.domain.user.entity.User;
import kr.ai_hub.AI_HUB_BE.domain.user.entity.UserRole;
import kr.ai_hub.AI_HUB_BE.domain.user.repository.UserRepository;
import kr.ai_hub.AI_HUB_BE.global.auth.SecurityContextHelper;
import kr.ai_hub.AI_HUB_BE.global.error.exception.ForbiddenException;
import kr.ai_hub.AI_HUB_BE.global.error.exception.ModelNotFoundException;
import kr.ai_hub.AI_HUB_BE.global.error.exception.UserNotFoundException;
import kr.ai_hub.AI_HUB_BE.global.error.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminAIModelService {

    private final AIModelRepository aiModelRepository;
    private final UserRepository userRepository;
    private final SecurityContextHelper securityContextHelper;

    /**
     * 새로운 AI 모델을 등록합니다 (관리자 전용).
     */
    @Transactional
    public AIModelResponse createModel(CreateAIModelRequest request) {
        validateAdminRole();
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
                .isActive(request.isActive())
                .build();

        AIModel savedModel = aiModelRepository.save(model);
        log.info("모델 생성 완료: modelId={}, modelName={}", savedModel.getModelId(), savedModel.getModelName());

        return AIModelResponse.from(savedModel);
    }

    /**
     * AI 모델 정보를 수정합니다 (관리자 전용).
     */
    @Transactional
    public AIModelResponse updateModel(Integer modelId, UpdateAIModelRequest request) {
        validateAdminRole();
        log.info("관리자 모델 수정 요청: modelId={}", modelId);

        AIModel model = aiModelRepository.findById(modelId)
                .orElseThrow(() -> new ModelNotFoundException("모델을 찾을 수 없습니다: " + modelId));

        model.update(
                request.displayName(),
                request.displayExplain(),
                request.inputPricePer1m(),
                request.outputPricePer1m(),
                request.isActive()
        );

        log.info("모델 수정 완료: modelId={}", modelId);

        return AIModelResponse.from(model);
    }

    /**
     * AI 모델을 삭제합니다 (관리자 전용).
     */
    @Transactional
    public void deleteModel(Integer modelId) {
        validateAdminRole();
        log.info("관리자 모델 삭제 요청: modelId={}", modelId);

        AIModel model = aiModelRepository.findById(modelId)
                .orElseThrow(() -> new ModelNotFoundException("모델을 찾을 수 없습니다: " + modelId));

        // 실제로는 참조 무결성 체크를 해야하지만, JPA가 자동으로 처리
        // Foreign Key 제약 조건 위반 시 DataIntegrityViolationException 발생
        aiModelRepository.delete(model);

        log.info("모델 삭제 완료: modelId={}", modelId);
    }

    /**
     * 현재 사용자가 관리자 권한을 가지고 있는지 검증합니다.
     */
    private void validateAdminRole() {
        Integer userId = securityContextHelper.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + userId));

        if (user.getRole() != UserRole.ROLE_ADMIN) {
            log.warn("관리자 권한 없음: userId={}, role={}", userId, user.getRole());
            throw new ForbiddenException("관리자 권한이 필요합니다");
        }
    }
}
