package kr.ai_hub.AI_HUB_BE.controller.admin.aimodel;

import jakarta.validation.Valid;
import kr.ai_hub.AI_HUB_BE.application.admin.aimodel.AdminAIModelService;
import kr.ai_hub.AI_HUB_BE.application.admin.aimodel.dto.CreateAIModelRequest;
import kr.ai_hub.AI_HUB_BE.application.admin.aimodel.dto.UpdateAIModelRequest;
import kr.ai_hub.AI_HUB_BE.application.aimodel.dto.AIModelResponse;
import kr.ai_hub.AI_HUB_BE.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/models")
@RequiredArgsConstructor
public class AdminAIModelController {

    private final AdminAIModelService adminAIModelService;

    /**
     * 새로운 AI 모델을 등록합니다 (관리자 전용).
     */
    @PostMapping
    public ResponseEntity<ApiResponse<AIModelResponse>> createModel(@Valid @RequestBody CreateAIModelRequest request) {
        log.info("관리자 모델 생성 API 호출: modelName={}", request.modelName());

        AIModelResponse response = adminAIModelService.createModel(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response));
    }

    /**
     * AI 모델 정보를 수정합니다 (관리자 전용).
     */
    @PutMapping("/{modelId}")
    public ResponseEntity<ApiResponse<AIModelResponse>> updateModel(
            @PathVariable Integer modelId,
            @Valid @RequestBody UpdateAIModelRequest request) {
        log.info("관리자 모델 수정 API 호출: modelId={}", modelId);

        AIModelResponse response = adminAIModelService.updateModel(modelId, request);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * AI 모델을 삭제합니다 (관리자 전용).
     */
    @DeleteMapping("/{modelId}")
    public ResponseEntity<Void> deleteModel(@PathVariable Integer modelId) {
        log.info("관리자 모델 삭제 API 호출: modelId={}", modelId);

        adminAIModelService.deleteModel(modelId);

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
