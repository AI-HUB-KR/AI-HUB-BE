package kr.ai_hub.AI_HUB_BE.chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import kr.ai_hub.AI_HUB_BE.chat.dto.AiStreamingResult;
import kr.ai_hub.AI_HUB_BE.chat.dto.AiUsage;
import kr.ai_hub.AI_HUB_BE.chat.dto.SseEvent;
import kr.ai_hub.AI_HUB_BE.global.error.exception.AIServerException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiSseHandler {

    @Qualifier("aiServerRestClient")
    private final RestClient aiServerRestClient;
    private final ObjectMapper objectMapper;

    /**
     * AI 서버로부터 SSE 스트리밍 응답을 처리합니다.
     * <p>
     * AI 서버 응답 형식 (표준 SSE):
     * - event: response, data: {"type":"response","data":"텍스트 조각"} 또는 data: "텍스트 조각"
     * - event: usage, data: {"input_tokens":50,"output_tokens":120,"total_tokens":170,"response_id":"resp_abc123"}
     * </p>
     */
    public AiStreamingResult stream(Map<String, Object> requestBody, SseEmitter emitter)
            throws JsonProcessingException, IOException, IllegalStateException {
        StringBuilder fullContent = new StringBuilder();
        AtomicReference<String> aiResponseId = new AtomicReference<>();
        AtomicReference<AiUsage> usage = new AtomicReference<>();

        try {
            return aiServerRestClient.post()
                    .uri("/ai/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.TEXT_EVENT_STREAM)
                    .body(requestBody)
                    .exchange((request, response) -> {
                        if (response.getStatusCode().isError()) {
                            String errorBody = readBodyAsString(response);
                            throw new AIServerException("AI 서버 통신 실패: " + errorBody);
                        }

                        InputStream body = response.getBody();
                        if (body == null) {
                            throw new AIServerException("AI 서버 응답이 비어 있습니다");
                        }

                        try (BufferedReader reader = new BufferedReader(
                                new InputStreamReader(body, StandardCharsets.UTF_8))) {
                            parseSseStream(reader, (eventType, data) ->
                                    handleSsePayload(eventType, data, emitter, fullContent, aiResponseId, usage));
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }

                        return new AiStreamingResult(aiResponseId.get(), fullContent.toString(), usage.get());
                    });
        } catch (UncheckedIOException e) {
            IOException cause = e.getCause();
            if (cause instanceof JsonProcessingException jsonProcessingException) {
                throw jsonProcessingException;
            }
            throw cause;
        }
    }

    /**
     * SSE 응답을 라인 단위로 읽어 이벤트 단위로 분리합니다.
     */
    private void parseSseStream(BufferedReader reader, SsePayloadHandler handler) throws IOException {
        String line;
        StringBuilder dataBuffer = new StringBuilder();
        String eventType = null;

        while ((line = reader.readLine()) != null) {
            if (line.isEmpty()) {
                if (dataBuffer.length() > 0) {
                    handler.handle(eventType, dataBuffer.toString());
                }
                dataBuffer.setLength(0);
                eventType = null;
                continue;
            }

            if (line.startsWith(":")) {
                continue;
            }

            if (line.startsWith("event:")) {
                eventType = line.substring("event:".length()).trim();
                continue;
            }

            if (line.startsWith("data:")) {
                appendData(dataBuffer, line.substring("data:".length()));
                continue;
            }

            appendData(dataBuffer, line);
        }

        if (dataBuffer.length() > 0) {
            handler.handle(eventType, dataBuffer.toString());
        }
    }

    /**
     * data 라인을 표준 규칙에 맞게 누적합니다.
     */
    private void appendData(StringBuilder dataBuffer, String dataLine) {
        String data = dataLine;
        if (data.startsWith(" ")) {
            data = data.substring(1);
        }
        if (dataBuffer.length() > 0) {
            dataBuffer.append('\n');
        }
        dataBuffer.append(data);
    }

    /**
     * 파싱된 SSE 이벤트를 타입별로 처리하고 emitter로 전달합니다.
     */
    private void handleSsePayload(String eventType, String data, SseEmitter emitter, StringBuilder fullContent,
                                  AtomicReference<String> aiResponseId, AtomicReference<AiUsage> usage)
            throws IOException {
        if (data == null || data.isBlank()) {
            return;
        }

        SseEvent event = parseSseEvent(eventType, data);
        if (event == null || event.type() == null) {
            log.warn("SSE 이벤트 파싱 실패: eventType={}, data={}", eventType, data);
            return;
        }

        switch (event.type()) {
            case "response":
                if (event.data() != null) {
                    String textChunk = event.data().toString();
                    fullContent.append(textChunk);
                    emitter.send(SseEmitter.event()
                            .name(event.type())
                            .data(event));
                }
                break;

            case "usage":
                log.info("AI 서버 usage 이벤트 수신");
                if (event.data() != null) {
                    SseEvent.UsageData usageData = objectMapper.convertValue(
                            event.data(), SseEvent.UsageData.class);
                    aiResponseId.set(usageData.responseId());
                    AiUsage usageValue = new AiUsage(
                            usageData.inputTokens(),
                            usageData.outputTokens(),
                            usageData.totalTokens()
                    );
                    usage.set(usageValue);
                    log.info("AI 응답 완료: responseId={}, tokens={}",
                            usageData.responseId(), usageValue.totalTokens());
                } else {
                    log.error("usage data가 null입니다!");
                }
                break;

            case "error":
                String errorMessage = resolveSseErrorMessage(data);
                log.error("AI 서버 error 이벤트 수신: {}", errorMessage);
                throw new AIServerException(errorMessage);

            default:
                log.warn("알 수 없는 이벤트 타입: {}", event.type());
                break;
        }
    }

    /**
     * SSE payload를 SseEvent로 변환합니다(JSON/텍스트 혼용 지원).
     */
    private SseEvent parseSseEvent(String eventType, String data) throws JsonProcessingException {
        String payload = data.trim();
        if (payload.isEmpty()) {
            return null;
        }

        if (payload.startsWith("{")) {
            try {
                SseEvent parsed = objectMapper.readValue(payload, SseEvent.class);
                if (parsed.type() == null && eventType != null) {
                    return new SseEvent(eventType, parsed.data() != null ? parsed.data() : payload);
                }
                return parsed;
            } catch (JsonProcessingException e) {
                if (eventType != null) {
                    if ("usage".equals(eventType)) {
                        SseEvent.UsageData usageData = objectMapper.readValue(payload, SseEvent.UsageData.class);
                        return new SseEvent(eventType, usageData);
                    }
                    return new SseEvent(eventType, payload);
                }
                throw e;
            }
        }

        if (eventType != null) {
            return new SseEvent(eventType, payload);
        }
        return null;
    }

    /**
     * SSE error payload에서 에러 메시지를 추출합니다.
     */
    private String resolveSseErrorMessage(String data) {
        String payload = data == null ? "" : data.trim();
        if (payload.isEmpty() || !payload.startsWith("{")) {
            return payload.isEmpty() ? "AI 서버 응답 에러" : payload;
        }

        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode errorNode = root.has("error") ? root.get("error") : root;
            String code = getJsonText(errorNode, "code");
            String message = getJsonText(errorNode, "message");

            if (message == null || message.isBlank()) {
                return "AI 서버 응답 에러";
            }
            if (code != null && !code.isBlank()) {
                return "[" + code + "] " + message;
            }
            return message;
        } catch (JsonProcessingException e) {
            return payload;
        }
    }

    private String getJsonText(JsonNode node, String field) {
        if (node == null) {
            return null;
        }
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        return value.asText();
    }

    /**
     * 에러 응답 바디를 문자열로 읽습니다.
     */
    private String readBodyAsString(ClientHttpResponse response) {
        try (InputStream body = response.getBody()) {
            if (body == null) {
                return "";
            }
            return new String(body.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("AI 서버 에러 응답 읽기 실패: {}", e.getMessage(), e);
            return "AI 서버 응답 에러";
        }
    }

    /**
     * 분리된 SSE 이벤트를 처리하는 콜백입니다.
     */
    @FunctionalInterface
    private interface SsePayloadHandler {
        void handle(String eventType, String data) throws IOException;
    }
}
