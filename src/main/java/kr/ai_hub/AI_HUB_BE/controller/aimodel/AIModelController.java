package kr.ai_hub.AI_HUB_BE.controller.aimodel;

import kr.ai_hub.AI_HUB_BE.application.aimodel.AIModelService;
import kr.ai_hub.AI_HUB_BE.application.aimodel.dto.AIModelResponse;
import kr.ai_hub.AI_HUB_BE.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/models")
@RequiredArgsConstructor
public class AIModelController {

    private final AIModelService aiModelService;

    /**
     * 활성화된 AI 모델 목록 조회
     * GET /api/v1/models
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<AIModelResponse>>> getActiveModels() {
        log.info("활성화된 AI 모델 목록 조회 API 호출");
        List<AIModelResponse> models = aiModelService.getActiveModels();
        return ResponseEntity.ok(ApiResponse.ok(models));
    }
}
