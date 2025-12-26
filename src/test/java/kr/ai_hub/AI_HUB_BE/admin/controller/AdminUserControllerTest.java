package kr.ai_hub.AI_HUB_BE.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.ai_hub.AI_HUB_BE.admin.service.AdminUserService;
import kr.ai_hub.AI_HUB_BE.admin.dto.ModifyUserAuthorityRequest;
import kr.ai_hub.AI_HUB_BE.admin.dto.ModifyUserWalletRequest;
import kr.ai_hub.AI_HUB_BE.admin.dto.UserListResponse;
import kr.ai_hub.AI_HUB_BE.user.domain.UserRole;
import kr.ai_hub.AI_HUB_BE.global.security.SecurityContextHelper;
import kr.ai_hub.AI_HUB_BE.global.security.jwt.JwtAuthenticationFilter;
import kr.ai_hub.AI_HUB_BE.global.security.jwt.JwtTokenProvider;
import kr.ai_hub.AI_HUB_BE.global.config.SecurityConfig;
import kr.ai_hub.AI_HUB_BE.global.error.exception.ForbiddenException;
import kr.ai_hub.AI_HUB_BE.global.error.exception.UserNotFoundException;
import kr.ai_hub.AI_HUB_BE.global.error.exception.WalletNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminUserController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientWebSecurityAutoConfiguration.class
}, excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityContextHelper.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class)
})
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminUserService adminUserService;

    @MockitoBean
    private SecurityContextHelper securityContextHelper;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("사용자 권한 수정 - 성공")
    void modifyUserAuthority_Success() throws Exception {
        // given
        ModifyUserAuthorityRequest request = new ModifyUserAuthorityRequest(1, UserRole.ROLE_ADMIN);
        given(securityContextHelper.getCurrentUserId()).willReturn(2);

        // when & then
        mockMvc.perform(patch("/api/v1/admin/authority")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("사용자 권한 수정 - 본인 권한 수정 시도")
    void modifyUserAuthority_SelfModification() throws Exception {
        // given
        ModifyUserAuthorityRequest request = new ModifyUserAuthorityRequest(1, UserRole.ROLE_ADMIN);
        given(securityContextHelper.getCurrentUserId()).willReturn(1);

        doThrow(new ForbiddenException("본인의 권한은 수정할 수 없습니다"))
                .when(adminUserService).modifyUserAuthority(eq(1), eq(UserRole.ROLE_ADMIN), eq(1));

        // when & then
        mockMvc.perform(patch("/api/v1/admin/authority")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("사용자 권한 수정 - 사용자 없음")
    void modifyUserAuthority_UserNotFound() throws Exception {
        // given
        ModifyUserAuthorityRequest request = new ModifyUserAuthorityRequest(999, UserRole.ROLE_ADMIN);
        given(securityContextHelper.getCurrentUserId()).willReturn(1);

        doThrow(new UserNotFoundException("사용자를 찾을 수 없습니다"))
                .when(adminUserService).modifyUserAuthority(eq(999), eq(UserRole.ROLE_ADMIN), eq(1));

        // when & then
        mockMvc.perform(patch("/api/v1/admin/authority")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("전체 사용자 정보 조회 - 성공")
    void getAllUsers_Success() throws Exception {
        // given
        UserListResponse response1 = new UserListResponse(
                1, "user1", UserRole.ROLE_USER, "user1@example.com",
                1, BigDecimal.valueOf(1000), BigDecimal.valueOf(800), BigDecimal.valueOf(200),
                BigDecimal.valueOf(2000), BigDecimal.valueOf(1000)
        );
        UserListResponse response2 = new UserListResponse(
                2, "user2", UserRole.ROLE_ADMIN, "user2@example.com",
                2, BigDecimal.valueOf(500), BigDecimal.valueOf(300), BigDecimal.valueOf(200),
                BigDecimal.valueOf(1000), BigDecimal.valueOf(500)
        );

        given(adminUserService.getAllUsersWithWallet()).willReturn(List.of(response1, response2));

        // when & then
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.detail").isArray())
                .andExpect(jsonPath("$.detail.length()").value(2))
                .andExpect(jsonPath("$.detail[0].userId").value(1))
                .andExpect(jsonPath("$.detail[0].username").value("user1"))
                .andExpect(jsonPath("$.detail[1].userId").value(2))
                .andExpect(jsonPath("$.detail[1].username").value("user2"));
    }

    @Test
    @DisplayName("프로모션 코인 수정 - 성공")
    void modifyUserWallet_Success() throws Exception {
        // given
        Integer userId = 1;
        BigDecimal promotionBalance = BigDecimal.valueOf(1000);

        ModifyUserWalletRequest request = new ModifyUserWalletRequest(userId, promotionBalance);

        given(securityContextHelper.getCurrentUserId()).willReturn(2);

        // when & then
        mockMvc.perform(patch("/api/v1/admin/wallet")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("프로모션 코인 수정 - 지갑 없음")
    void modifyUserWallet_WalletNotFound() throws Exception {
        // given
        Integer userId = 999;
        BigDecimal promotionBalance = BigDecimal.valueOf(1000);

        ModifyUserWalletRequest request = new ModifyUserWalletRequest(userId, promotionBalance);

        given(securityContextHelper.getCurrentUserId()).willReturn(2);

        doThrow(new WalletNotFoundException("지갑을 찾을 수 없습니다"))
                .when(adminUserService).modifyPromotionBalance(eq(userId), any(BigDecimal.class), any(Integer.class));

        // when & then
        mockMvc.perform(patch("/api/v1/admin/wallet")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }
}
