package kr.ai_hub.AI_HUB_BE.controller.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import kr.ai_hub.AI_HUB_BE.application.user.UserService;
import kr.ai_hub.AI_HUB_BE.application.user.dto.UpdateUserRequest;
import kr.ai_hub.AI_HUB_BE.application.user.dto.UserResponse;
import kr.ai_hub.AI_HUB_BE.global.auth.SecurityContextHelper;
import kr.ai_hub.AI_HUB_BE.global.error.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

        private MockMvc mockMvc;

        @Mock
        private UserService userService;

        @Mock
        private SecurityContextHelper securityContextHelper;

        @InjectMocks
        private UserController userController;

        private ObjectMapper objectMapper;

        @BeforeEach
        void setUp() {
                objectMapper = new ObjectMapper();
                objectMapper.registerModule(new JavaTimeModule());

                mockMvc = MockMvcBuilders.standaloneSetup(userController)
                                .setControllerAdvice(new GlobalExceptionHandler(securityContextHelper))
                                .build();
        }

        @Test
        @DisplayName("내 정보 조회")
        void getCurrentUser() throws Exception {
                // given
                UserResponse response = UserResponse.builder()
                                .userId(1)
                                .username("testuser")
                                .email("test@example.com")
                                .isActivated(true)
                                .createdAt(Instant.now())
                                .build();

                given(userService.getCurrentUser()).willReturn(response);

                // when & then
                mockMvc.perform(get("/api/v1/users/me"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.detail.userId").value(1))
                                .andExpect(jsonPath("$.detail.username").value("testuser"))
                                .andExpect(jsonPath("$.detail.email").value("test@example.com"));
        }

        @Test
        @DisplayName("내 정보 수정")
        void updateCurrentUser() throws Exception {
                // given
                UpdateUserRequest request = new UpdateUserRequest("updatedUser", "updated@example.com");
                UserResponse response = UserResponse.builder()
                                .userId(1)
                                .username("updatedUser")
                                .email("updated@example.com")
                                .isActivated(true)
                                .createdAt(Instant.now())
                                .build();

                given(userService.updateCurrentUser(any(UpdateUserRequest.class))).willReturn(response);

                // when & then
                mockMvc.perform(put("/api/v1/users/me")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.detail.username").value("updatedUser"))
                                .andExpect(jsonPath("$.detail.email").value("updated@example.com"));
        }

        @Test
        @DisplayName("회원 탈퇴")
        void deleteCurrentUser() throws Exception {
                // when & then
                mockMvc.perform(delete("/api/v1/users/me"))
                                .andExpect(status().isNoContent());

                verify(userService).deleteCurrentUser();
        }
}
