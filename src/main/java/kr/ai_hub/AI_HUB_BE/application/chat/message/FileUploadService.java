package kr.ai_hub.AI_HUB_BE.application.chat.message;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.ai_hub.AI_HUB_BE.application.chat.message.dto.AiServerResponse;
import kr.ai_hub.AI_HUB_BE.application.chat.message.dto.AiUploadData;
import kr.ai_hub.AI_HUB_BE.application.chat.message.dto.FileUploadResponse;
import kr.ai_hub.AI_HUB_BE.domain.aimodel.AIModel;
import kr.ai_hub.AI_HUB_BE.domain.aimodel.AIModelRepository;
import kr.ai_hub.AI_HUB_BE.global.error.exception.AIServerException;
import kr.ai_hub.AI_HUB_BE.global.error.exception.ModelNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FileUploadService {

    private final FileValidationService fileValidationService;
    private final AIModelRepository aiModelRepository;
    @Qualifier("aiServerUploadClient")
    private final RestClient aiServerUploadClient;
    private final ObjectMapper objectMapper;

    /**
     * 파일을 AI 서버에 업로드합니다.
     *
     * @param file    업로드할 파일
     * @param modelId AI 모델 ID
     * @return 파일 업로드 응답 (file ID)
     */
    public FileUploadResponse uploadFile(MultipartFile file, Integer modelId) {
        log.info("파일 업로드 시작: fileName={}, size={}, modelId={}",
                file.getOriginalFilename(), file.getSize(), modelId);

        fileValidationService.validateFile(file);

        AIModel aiModel = aiModelRepository.findById(modelId)
                .orElseThrow(() -> new ModelNotFoundException("AI 모델을 찾을 수 없습니다: " + modelId));
        log.debug("AI 모델 조회 성공: modelName={}", aiModel.getModelName());

        try {
            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("file", file.getResource());
            MultiValueMap<String, HttpEntity<?>> multipartBody = builder.build();

            AiServerResponse<AiUploadData> response = aiServerUploadClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/ai/upload")
                            .queryParam("model", aiModel.getModelName())
                            .build())
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(multipartBody)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, clientResponse) -> {
                        String errorMessage = resolveErrorMessage(clientResponse);
                        log.error("AI 서버 파일 업로드 실패: {}", errorMessage);
                        throw new AIServerException(errorMessage);
                    })
                    .body(new ParameterizedTypeReference<AiServerResponse<AiUploadData>>() {
                    });

            if (response == null || !response.success() || response.data() == null) {
                log.error("AI 서버 응답 없음 또는 실패");
                throw new AIServerException("AI 서버로부터 응답을 받지 못했습니다");
            }

            AiUploadData uploadData = response.data();
            String fileId = uploadData.fileId();
            log.info("파일 업로드 성공: fileId={}", fileId);

            return FileUploadResponse.of(fileId);

        } catch (Exception e) {
            log.error("파일 업로드 중 에러 발생: {}", e.getMessage(), e);
            if (e instanceof AIServerException) {
                throw e;
            }
            throw new AIServerException("파일 업로드 중 에러가 발생했습니다: " + e.getMessage(), e);
        }
    }

    private String resolveErrorMessage(ClientHttpResponse clientResponse) {
        try (var body = clientResponse.getBody()) {
            if (body == null) {
                return "AI 서버 응답 에러";
            }
            AiServerResponse<AiUploadData> errorResponse = objectMapper.readValue(
                    body,
                    new TypeReference<AiServerResponse<AiUploadData>>() {
                    });
            if (errorResponse != null && errorResponse.error() != null) {
                return errorResponse.error().message();
            }
        } catch (IOException e) {
            log.warn("AI 서버 에러 응답 파싱 실패: {}", e.getMessage(), e);
        }
        return "AI 서버 응답 에러";
    }
}
