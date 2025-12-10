package kr.ai_hub.AI_HUB_BE.application.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.ai_hub.AI_HUB_BE.application.chat.message.dto.AiUsage;
import kr.ai_hub.AI_HUB_BE.application.chat.message.dto.SseEvent;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class StreamAiResponseParsingTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void sseEventsAreParsedCorrectly() throws IOException {
        // 새로운 응답 형식: response 이벤트와 usage 이벤트
        List<String> lines = List.of(
                "data: {\"type\":\"response\",\"data\":\"안녕하세요\"}",
                "data: {\"type\":\"response\",\"data\":\"!\"}",
                "data: {\"type\":\"response\",\"data\":\" 무엇을 도와드릴까요?\"}",
                "data: {\"type\":\"usage\",\"data\":{\"input_tokens\":10,\"output_tokens\":15,\"total_tokens\":25,\"response_id\":\"resp_abc123\"}}"
        );

        StringBuilder fullContent = new StringBuilder();
        AiUsage usage = null;
        String aiResponseId = null;

        for (String rawLine : lines) {
            if (!rawLine.startsWith("data: ")) {
                continue;
            }

            String jsonData = rawLine.substring(6);
            SseEvent event = objectMapper.readValue(jsonData, SseEvent.class);

            switch (event.type()) {
                case "response" -> {
                    assertNotNull(event.data());
                    fullContent.append(event.data().toString());
                }
                case "usage" -> {
                    assertNotNull(event.data());
                    SseEvent.UsageData usageData = objectMapper.convertValue(
                            event.data(), SseEvent.UsageData.class);
                    aiResponseId = usageData.responseId();
                    usage = new AiUsage(
                            usageData.inputTokens(),
                            usageData.outputTokens(),
                            usageData.totalTokens()
                    );
                }
            }
        }

        assertEquals("resp_abc123", aiResponseId);
        assertEquals("안녕하세요! 무엇을 도와드릴까요?", fullContent.toString());
        assertNotNull(usage);
        assertEquals(10, usage.inputTokens());
        assertEquals(15, usage.outputTokens());
        assertEquals(25, usage.totalTokens());
    }
}
