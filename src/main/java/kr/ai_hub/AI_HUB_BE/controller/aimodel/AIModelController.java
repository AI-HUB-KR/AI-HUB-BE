package kr.ai_hub.AI_HUB_BE.controller.aimodel;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "AI 모델", description = "AI 모델 정보 조회")
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
    @Operation(summary = "활성화된 AI 모델 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<AIModelResponse>>> getActiveModels() {
        log.info("활성화된 AI 모델 목록 조회 API 호출");
        List<AIModelResponse> models = aiModelService.getActiveModels();
        return ResponseEntity.ok(ApiResponse.ok(models));
    }
}
