package kr.ai_hub.AI_HUB_BE.controller.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.ai_hub.AI_HUB_BE.application.chat.chatroom.ChatRoomService;
import kr.ai_hub.AI_HUB_BE.application.chat.chatroom.dto.ChatRoomListItemResponse;
import kr.ai_hub.AI_HUB_BE.application.chat.chatroom.dto.ChatRoomResponse;
import kr.ai_hub.AI_HUB_BE.application.chat.chatroom.dto.CreateChatRoomRequest;
import kr.ai_hub.AI_HUB_BE.application.chat.chatroom.dto.UpdateChatRoomRequest;
import kr.ai_hub.AI_HUB_BE.global.auth.SecurityContextHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ChatRoomController.class, excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                OAuth2ClientAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientWebSecurityAutoConfiguration.class
}, excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityContextHelper.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = kr.ai_hub.AI_HUB_BE.global.auth.jwt.JwtAuthenticationFilter.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = kr.ai_hub.AI_HUB_BE.global.config.SecurityConfig.class)
})
class ChatRoomControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private ChatRoomService chatRoomService;

        @MockBean
        private SecurityContextHelper securityContextHelper;

        @MockBean
        private kr.ai_hub.AI_HUB_BE.global.auth.jwt.JwtTokenProvider jwtTokenProvider;

        @Autowired
        private ObjectMapper objectMapper;

        @Test
        @DisplayName("채팅방 생성")
        void createChatRoom() throws Exception {
                // given
                CreateChatRoomRequest request = new CreateChatRoomRequest("New Room", 1);
                ChatRoomResponse response = ChatRoomResponse.builder()
                                .roomId(UUID.randomUUID().toString())
                                .title("New Room")
                                .userId(1)
                                .coinUsage(BigDecimal.ZERO)
                                .createdAt(Instant.now())
                                .updatedAt(Instant.now())
                                .build();

                given(chatRoomService.createChatRoom(any(CreateChatRoomRequest.class))).willReturn(response);

                // when & then
                mockMvc.perform(post("/api/v1/chat-rooms")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.detail.title").value("New Room"));
        }

        @Test
        @DisplayName("채팅방 목록 조회")
        void getChatRooms() throws Exception {
                // given
                ChatRoomListItemResponse item = ChatRoomListItemResponse.builder()
                                .roomId(UUID.randomUUID().toString())
                                .title("Room 1")
                                .coinUsage(BigDecimal.ZERO)
                                .createdAt(Instant.now())
                                .build();
                Page<ChatRoomListItemResponse> page = new PageImpl<>(List.of(item));

                given(chatRoomService.getChatRooms(any(Pageable.class))).willReturn(page);

                // when & then
                mockMvc.perform(get("/api/v1/chat-rooms")
                                .param("page", "0")
                                .param("size", "10"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.detail.content[0].title").value("Room 1"));
        }

        @Test
        @DisplayName("채팅방 상세 조회")
        void getChatRoom() throws Exception {
                // given
                UUID roomId = UUID.randomUUID();
                ChatRoomResponse response = ChatRoomResponse.builder()
                                .roomId(roomId.toString())
                                .title("Room Detail")
                                .userId(1)
                                .coinUsage(BigDecimal.ZERO)
                                .createdAt(Instant.now())
                                .updatedAt(Instant.now())
                                .build();

                given(chatRoomService.getChatRoom(roomId)).willReturn(response);

                // when & then
                mockMvc.perform(get("/api/v1/chat-rooms/{roomId}", roomId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.detail.title").value("Room Detail"));
        }

        @Test
        @DisplayName("채팅방 제목 수정")
        void updateChatRoom() throws Exception {
                // given
                UUID roomId = UUID.randomUUID();
                UpdateChatRoomRequest request = new UpdateChatRoomRequest("Updated Room");
                ChatRoomResponse response = ChatRoomResponse.builder()
                                .roomId(roomId.toString())
                                .title("Updated Room")
                                .userId(1)
                                .coinUsage(BigDecimal.ZERO)
                                .createdAt(Instant.now())
                                .updatedAt(Instant.now())
                                .build();

                given(chatRoomService.updateChatRoom(eq(roomId), any(UpdateChatRoomRequest.class)))
                                .willReturn(response);

                // when & then
                mockMvc.perform(put("/api/v1/chat-rooms/{roomId}", roomId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.detail.title").value("Updated Room"));
        }

        @Test
        @DisplayName("채팅방 삭제")
        void deleteChatRoom() throws Exception {
                // given
                UUID roomId = UUID.randomUUID();

                // when & then
                mockMvc.perform(delete("/api/v1/chat-rooms/{roomId}", roomId))
                                .andExpect(status().isNoContent());

                verify(chatRoomService).deleteChatRoom(roomId);
        }
}
