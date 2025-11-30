package kr.ai_hub.AI_HUB_BE.controller.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.ai_hub.AI_HUB_BE.application.chat.message.MessageService;
import kr.ai_hub.AI_HUB_BE.application.chat.message.dto.FileUploadResponse;
import kr.ai_hub.AI_HUB_BE.application.chat.message.dto.MessageListItemResponse;
import kr.ai_hub.AI_HUB_BE.application.chat.message.dto.MessageResponse;
import kr.ai_hub.AI_HUB_BE.application.chat.message.dto.SendMessageRequest;
import kr.ai_hub.AI_HUB_BE.global.auth.SecurityContextHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ChatMessageController.class, excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        OAuth2ClientAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientWebSecurityAutoConfiguration.class
}, excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityContextHelper.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = kr.ai_hub.AI_HUB_BE.global.auth.jwt.JwtAuthenticationFilter.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = kr.ai_hub.AI_HUB_BE.global.config.SecurityConfig.class)
})
class ChatMessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MessageService messageService;

    @MockBean
    private SecurityContextHelper securityContextHelper;

    @MockBean
    private kr.ai_hub.AI_HUB_BE.global.auth.jwt.JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("메시지 전송 (SSE 스트리밍)")
    void sendMessage() throws Exception {
        // given
        UUID roomId = UUID.randomUUID();
        SendMessageRequest request = SendMessageRequest.builder()
                .message("Hello AI")
                .modelId(1)
                .build();

        willDoNothing().given(messageService).sendMessage(eq(roomId), any(SendMessageRequest.class),
                any(SseEmitter.class));

        // when & then
        mockMvc.perform(post("/api/v1/messages/send/{roomId}", roomId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("파일 업로드")
    void uploadFile() throws Exception {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "test image content".getBytes());
        Integer modelId = 1;
        FileUploadResponse response = FileUploadResponse.builder()
                .fileId("file-123")
                .build();

        given(messageService.uploadFile(any(), eq(modelId))).willReturn(response);

        // when & then
        mockMvc.perform(multipart("/api/v1/messages/files/upload")
                .file(file)
                .param("modelId", String.valueOf(modelId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.detail.fileId").value("file-123"));
    }

    @Test
    @DisplayName("메시지 목록 조회")
    void getMessages() throws Exception {
        // given
        UUID roomId = UUID.randomUUID();
        MessageListItemResponse messageRes = MessageListItemResponse.builder()
                .messageId("msg-1")
                .role("user")
                .content("Hello")
                .tokenCount(BigDecimal.TEN)
                .coinCount(BigDecimal.ONE)
                .modelId(1)
                .createdAt(Instant.now())
                .build();
        Page<MessageListItemResponse> page = new PageImpl<>(List.of(messageRes));

        given(messageService.getMessages(eq(roomId), any(Pageable.class))).willReturn(page);

        // when & then
        mockMvc.perform(get("/api/v1/messages/page/{roomId}", roomId)
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.detail.content[0].messageId").value("msg-1"));
    }

    @Test
    @DisplayName("메시지 상세 조회")
    void getMessage() throws Exception {
        // given
        UUID messageId = UUID.randomUUID();
        MessageResponse response = MessageResponse.builder()
                .messageId(messageId.toString())
                .roomId(UUID.randomUUID().toString())
                .role("assistant")
                .content("Hi there")
                .build();

        given(messageService.getMessage(messageId)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/messages/{messageId}", messageId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.detail.messageId").value(messageId.toString()))
                .andExpect(jsonPath("$.detail.content").value("Hi there"));
    }
}
