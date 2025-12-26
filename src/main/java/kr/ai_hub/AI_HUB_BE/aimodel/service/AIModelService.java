package kr.ai_hub.AI_HUB_BE.aimodel.service;

import kr.ai_hub.AI_HUB_BE.aimodel.dto.AIModelResponse;
import kr.ai_hub.AI_HUB_BE.aimodel.domain.AIModelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AIModelService {

    private final AIModelRepository aiModelRepository;

    /**
     * 활성화된 AI 모델 목록을 조회합니다.
     */
    public List<AIModelResponse> getActiveModels() {
        log.debug("활성화된 AI 모델 목록 조회");

        return aiModelRepository.findByIsActiveTrueOrderByCreatedAtDesc()
                .stream()
                .map(AIModelResponse::from)
                .collect(Collectors.toList());
    }
}
