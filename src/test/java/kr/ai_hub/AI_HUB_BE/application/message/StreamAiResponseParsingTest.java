package kr.ai_hub.AI_HUB_BE.application.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.ai_hub.AI_HUB_BE.application.message.dto.AiUsage;
import kr.ai_hub.AI_HUB_BE.application.message.dto.SseEvent;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StreamAiResponseParsingTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void sseEventsAreParsedCorrectly() throws IOException {
        List<String> lines = List.of(
                "data: {\"type\":\"response.created\",\"response\":{\"id\":\"resp_abc123\"}}",
                "data: {\"type\":\"response.output_text.delta\",\"delta\":\"안녕하세요\",\"sequence_number\":1}",
                "data: {\"type\":\"response.output_text.delta\",\"delta\":\"!\",\"sequence_number\":2}",
                "data: {\"type\":\"response.completed\",\"sequence_number\":3," +
                        "\"response\":{\"id\":\"resp_abc123\",\"model\":\"gpt-5-mini\"," +
                        "\"content\":\"안녕하세요!\",\"usage\":{\"input_tokens\":10,\"output_tokens\":5,\"total_tokens\":15}}}"
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
                case "response.created" -> aiResponseId = event.response() != null ? event.response().id() : null;
                case "response.output_text.delta" -> {
                    assertNotNull(event.delta());
                    fullContent.append(event.delta());
                }
                case "response.completed" -> {
                    assertNotNull(event.response());
                    usage = event.response().usage();
                }
            }
        }

        assertEquals("resp_abc123", aiResponseId);
        assertEquals("안녕하세요!", fullContent.toString());
        assertNotNull(usage);
        assertEquals(10, usage.inputTokens());
        assertEquals(5, usage.outputTokens());
        assertEquals(15, usage.totalTokens());
    }
}
