package kr.ai_hub.AI_HUB_BE.domain.user.repository;

import kr.ai_hub.AI_HUB_BE.domain.user.UserRepository;

import kr.ai_hub.AI_HUB_BE.domain.user.User;
import kr.ai_hub.AI_HUB_BE.domain.user.UserRole;
import kr.ai_hub.AI_HUB_BE.global.config.TestConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestConfig.class)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("사용자 저장 및 조회 테스트")
    void saveAndFindUser() {
        // given
        User user = User.builder()
                .username("testuser")
                .email("test@example.com")
                .role(UserRole.ROLE_USER)
                .isActivated(true)
                .isDeleted(false)
                .build();

        // when
        User savedUser = userRepository.save(user);

        // then
        assertThat(savedUser.getUserId()).isNotNull();
        assertThat(savedUser.getUsername()).isEqualTo("testuser");
        assertThat(savedUser.getEmail()).isEqualTo("test@example.com");
        assertThat(savedUser.getCreatedAt()).isNotNull(); // Auditing check
    }

    @Test
    @DisplayName("Username으로 사용자 조회")
    void findByUsername() {
        // given
        User user = User.builder()
                .username("findme")
                .email("findme@example.com")
                .role(UserRole.ROLE_USER)
                .build();
        userRepository.save(user);

        // when
        Optional<User> foundUser = userRepository.findByUsername("findme");

        // then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo("findme@example.com");
    }

    @Test
    @DisplayName("Email로 사용자 조회")
    void findByEmail() {
        // given
        User user = User.builder()
                .username("emailuser")
                .email("email@example.com")
                .role(UserRole.ROLE_USER)
                .build();
        userRepository.save(user);

        // when
        Optional<User> foundUser = userRepository.findByEmail("email@example.com");

        // then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUsername()).isEqualTo("emailuser");
    }

    @Test
    @DisplayName("Username 존재 여부 확인")
    void existsByUsername() {
        // given
        User user = User.builder()
                .username("existuser")
                .email("exist@example.com")
                .role(UserRole.ROLE_USER)
                .build();
        userRepository.save(user);

        // when
        boolean exists = userRepository.existsByUsername("existuser");
        boolean notExists = userRepository.existsByUsername("unknown");

        // then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("Email 존재 여부 확인")
    void existsByEmail() {
        // given
        User user = User.builder()
                .username("existemail")
                .email("existemail@example.com")
                .role(UserRole.ROLE_USER)
                .build();
        userRepository.save(user);

        // when
        boolean exists = userRepository.existsByEmail("existemail@example.com");
        boolean notExists = userRepository.existsByEmail("unknown@example.com");

        // then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("KakaoId로 사용자 조회")
    void findByKakaoId() {
        // given
        User user = User.builder()
                .username("kakaouser")
                .email("kakao@example.com")
                .role(UserRole.ROLE_USER)
                .kakaoId("123456789")
                .build();
        userRepository.save(user);

        // when
        Optional<User> foundUser = userRepository.findByKakaoId("123456789");

        // then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUsername()).isEqualTo("kakaouser");
    }
}
