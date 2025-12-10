package kr.ai_hub.AI_HUB_BE.application.chat.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.ai_hub.AI_HUB_BE.application.chat.message.dto.AiStreamingResult;
import kr.ai_hub.AI_HUB_BE.application.chat.message.dto.AiUsage;
import kr.ai_hub.AI_HUB_BE.application.chat.message.dto.SseEvent;
import kr.ai_hub.AI_HUB_BE.global.error.exception.AIServerException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiSseHandler {

    private final WebClient aiServerWebClient;
    private final ObjectMapper objectMapper;

    /**
     * AI 서버로부터 SSE 스트리밍 응답을 처리합니다.
     * <p>
     * AI 서버 응답 형식:
     * - response 이벤트: {"type":"response","data":"텍스트 조각"}
     * - usage 이벤트: {"type":"usage","data":{"input_tokens":50,"output_tokens":120,"total_tokens":170,"response_id":"resp_abc123"}}
     * </p>
     */
    public AiStreamingResult stream(Map<String, Object> requestBody, SseEmitter emitter)
            throws JsonProcessingException, IOException, IllegalStateException {
        String aiResponseId = null;
        StringBuilder fullContent = new StringBuilder();
        AiUsage usage = null;

        var stream = aiServerWebClient.post()
                .uri("/ai/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .toStream();

        for (String line : (Iterable<String>) stream::iterator) {
            if (line == null || line.isBlank()) {
                continue;
            }
            SseEvent event = objectMapper.readValue(line, SseEvent.class);

            switch (event.type()) {
                case "response":
                    if (event.data() != null) {
                        String textChunk = event.data().toString();
                        fullContent.append(textChunk);
                        emitter.send(SseEmitter.event()
                                .name("delta")
                                .data(textChunk));
                    }
                    break;

                case "usage":
                    log.info("AI 서버 usage 이벤트 수신");
                    if (event.data() != null) {
                        SseEvent.UsageData usageData = objectMapper.convertValue(
                                event.data(), SseEvent.UsageData.class);
                        aiResponseId = usageData.responseId();
                        usage = new AiUsage(
                                usageData.inputTokens(),
                                usageData.outputTokens(),
                                usageData.totalTokens()
                        );
                        log.info("AI 응답 완료: responseId={}, tokens={}", aiResponseId, usage.totalTokens());
                    } else {
                        log.error("usage data가 null입니다!");
                    }
                    break;

                default:
                    log.warn("알 수 없는 이벤트 타입: {}", event.type());
                    break;
            }
        }

        return new AiStreamingResult(aiResponseId, fullContent.toString(), usage);
    }
}
