package kr.ai_hub.AI_HUB_BE.application.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.ai_hub.AI_HUB_BE.application.chat.message.dto.SseEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SseEventDeserializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void usageEventContainsUsageInformation() throws Exception {
        String json = """
            {
              "type":"usage",
              "data":{
                "input_tokens":50,
                "output_tokens":120,
                "total_tokens":170,
                "response_id":"resp_abc123"
              }
            }
            """;

        SseEvent event = objectMapper.readValue(json, SseEvent.class);

        assertEquals("usage", event.type());
        assertNotNull(event.data(), "data 객체가 역직렬화되어야 합니다");

        SseEvent.UsageData usageData = objectMapper.convertValue(event.data(), SseEvent.UsageData.class);
        assertNotNull(usageData, "usageData 객체가 역직렬화되어야 합니다");
        assertEquals(50, usageData.inputTokens());
        assertEquals(120, usageData.outputTokens());
        assertEquals(170, usageData.totalTokens());
        assertEquals("resp_abc123", usageData.responseId());
    }

    @Test
    void responseEventContainsTextData() throws Exception {
        String json = """
            {
              "type":"response",
              "data":"안녕하세요"
            }
            """;

        SseEvent event = objectMapper.readValue(json, SseEvent.class);

        assertEquals("response", event.type());
        assertEquals("안녕하세요", event.data());
    }
}
