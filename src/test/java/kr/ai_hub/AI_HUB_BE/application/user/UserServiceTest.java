package kr.ai_hub.AI_HUB_BE.application.user;

import kr.ai_hub.AI_HUB_BE.application.user.dto.UpdateUserRequest;
import kr.ai_hub.AI_HUB_BE.application.user.dto.UserResponse;
import kr.ai_hub.AI_HUB_BE.domain.user.User;
import kr.ai_hub.AI_HUB_BE.domain.user.UserRepository;
import kr.ai_hub.AI_HUB_BE.domain.user.UserRole;
import kr.ai_hub.AI_HUB_BE.global.auth.SecurityContextHelper;
import kr.ai_hub.AI_HUB_BE.global.error.exception.UserNotFoundException;
import kr.ai_hub.AI_HUB_BE.global.error.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityContextHelper securityContextHelper;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .userId(1)
                .username("testuser")
                .email("test@example.com")
                .role(UserRole.ROLE_USER)
                .isDeleted(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("현재 사용자 조회 성공")
    void getCurrentUser_Success() {
        // given
        given(securityContextHelper.getCurrentUserId()).willReturn(1);
        given(userRepository.findById(1)).willReturn(Optional.of(user));

        // when
        UserResponse response = userService.getCurrentUser();

        // then
        assertThat(response.userId()).isEqualTo(1);
        assertThat(response.username()).isEqualTo("testuser");
        assertThat(response.email()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("현재 사용자 조회 실패 - 사용자 없음")
    void getCurrentUser_UserNotFound() {
        // given
        given(securityContextHelper.getCurrentUserId()).willReturn(1);
        given(userRepository.findById(1)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.getCurrentUser())
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("사용자를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("사용자 정보 수정 성공")
    void updateCurrentUser_Success() {
        // given
        UpdateUserRequest request = new UpdateUserRequest("newuser", "new@example.com");
        given(securityContextHelper.getCurrentUserId()).willReturn(1);
        given(userRepository.findById(1)).willReturn(Optional.of(user));
        given(userRepository.existsByEmail("new@example.com")).willReturn(false);
        given(userRepository.existsByUsername("newuser")).willReturn(false);

        // when
        UserResponse response = userService.updateCurrentUser(request);

        // then
        assertThat(response.username()).isEqualTo("newuser");
        assertThat(response.email()).isEqualTo("new@example.com");
    }

    @Test
    @DisplayName("사용자 정보 수정 실패 - 이메일 중복")
    void updateCurrentUser_DuplicateEmail() {
        // given
        UpdateUserRequest request = new UpdateUserRequest("newuser", "duplicate@example.com");
        given(securityContextHelper.getCurrentUserId()).willReturn(1);
        given(userRepository.findById(1)).willReturn(Optional.of(user));
        given(userRepository.existsByEmail("duplicate@example.com")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> userService.updateCurrentUser(request))
                .isInstanceOf(ValidationException.class)
                .hasMessage("이미 사용 중인 이메일입니다");
    }

    @Test
    @DisplayName("사용자 정보 수정 실패 - 사용자명 중복")
    void updateCurrentUser_DuplicateUsername() {
        // given
        UpdateUserRequest request = new UpdateUserRequest("duplicateuser", "new@example.com");
        given(securityContextHelper.getCurrentUserId()).willReturn(1);
        given(userRepository.findById(1)).willReturn(Optional.of(user));
        given(userRepository.existsByEmail("new@example.com")).willReturn(false);
        given(userRepository.existsByUsername("duplicateuser")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> userService.updateCurrentUser(request))
                .isInstanceOf(ValidationException.class)
                .hasMessage("이미 사용 중인 사용자명입니다");
    }

    @Test
    @DisplayName("회원 탈퇴 성공")
    void deleteCurrentUser_Success() {
        // given
        given(securityContextHelper.getCurrentUserId()).willReturn(1);
        given(userRepository.findById(1)).willReturn(Optional.of(user));

        // when
        userService.deleteCurrentUser();

        // then
        assertThat(user.getIsDeleted()).isTrue();
    }
}
