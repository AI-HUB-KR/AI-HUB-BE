package kr.ai_hub.AI_HUB_BE.application.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.ai_hub.AI_HUB_BE.application.chat.message.dto.AiUsage;
import kr.ai_hub.AI_HUB_BE.application.chat.message.dto.SseEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SseEventDeserializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void responseCompletedEventContainsUsageInformation() throws Exception {
        String json = """
            {
              "type":"response.completed",
              "sequence_number":30,
              "response":{
                "id":"resp_00158610383aa76800691dc86323b08195864c68c6110879f4",
                "model":"gpt-5-mini-2025-08-07",
                "content":"안녕하세요! 무엇을 도와드릴까요? 한국어로 도와드릴게요.",
                "usage":{
                  "input_tokens":8,
                  "output_tokens":91,
                  "total_tokens":99
                }
              }
            }
            """;

        SseEvent event = objectMapper.readValue(json, SseEvent.class);

        assertEquals("response.completed", event.type());
        assertNotNull(event.response(), "response 객체가 역직렬화되어야 합니다");

        AiUsage usage = event.response().usage();
        assertNotNull(usage, "usage 객체가 역직렬화되어야 합니다");
        assertEquals(8, usage.inputTokens());
        assertEquals(91, usage.outputTokens());
        assertEquals(99, usage.totalTokens());
    }
}
