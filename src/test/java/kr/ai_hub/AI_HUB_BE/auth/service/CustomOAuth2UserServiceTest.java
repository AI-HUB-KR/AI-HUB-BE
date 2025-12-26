package kr.ai_hub.AI_HUB_BE.auth.service;

import kr.ai_hub.AI_HUB_BE.user.domain.User;
import kr.ai_hub.AI_HUB_BE.user.domain.UserRepository;
import kr.ai_hub.AI_HUB_BE.user.domain.UserRole;
import kr.ai_hub.AI_HUB_BE.global.security.oauth2.CustomOAuth2User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.client.RestOperations;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2UserServiceTest {

    @InjectMocks
    private CustomOAuth2UserService customOAuth2UserService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RestOperations restOperations;

    private ClientRegistration clientRegistration;
    private OAuth2UserRequest userRequest;
    private Map<String, Object> attributes;

    @BeforeEach
    void setUp() {
        customOAuth2UserService.setRestOperations(restOperations);

        clientRegistration = ClientRegistration.withRegistrationId("kakao")
                .clientId("client-id")
                .clientSecret("client-secret")
                .clientAuthenticationMethod(
                        org.springframework.security.oauth2.core.ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost:8080/login/oauth2/code/kakao")
                .scope("profile_nickname", "account_email")
                .authorizationUri("https://kauth.kakao.com/oauth/authorize")
                .tokenUri("https://kauth.kakao.com/oauth/token")
                .userInfoUri("https://kapi.kakao.com/v2/user/me")
                .userNameAttributeName("id")
                .clientName("Kakao")
                .build();

        userRequest = new OAuth2UserRequest(
                clientRegistration,
                new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "access-token", null, null));

        attributes = Map.of(
                "id", 123456789L,
                "kakao_account", Map.of(
                        "email", "test@example.com",
                        "profile", Map.of("nickname", "Test User", "profile_image_url", "http://image.url")));
    }

    @Test
    @DisplayName("기존 사용자 로그인 - DB 조회 후 반환")
    void loadUser_ExistingUser() {
        // given
        given(restOperations.exchange(any(org.springframework.http.RequestEntity.class),
                any(ParameterizedTypeReference.class)))
                .willReturn(new ResponseEntity<>(attributes, HttpStatus.OK));

        User existingUser = User.builder()
                .userId(1)
                .email("test@example.com")
                .kakaoId("123456789")
                .role(UserRole.ROLE_USER)
                .build();

        given(userRepository.findByKakaoId("123456789")).willReturn(Optional.of(existingUser));

        // when
        OAuth2User result = customOAuth2UserService.loadUser(userRequest);

        // then
        assertThat(result).isInstanceOf(CustomOAuth2User.class);
        CustomOAuth2User customUser = (CustomOAuth2User) result;
        assertThat(customUser.getUser()).isEqualTo(existingUser);
        verify(userRepository).findByKakaoId("123456789");
    }

    @Test
    @DisplayName("신규 사용자 로그인 - DB 저장 후 반환")
    void loadUser_NewUser() {
        // given
        given(restOperations.exchange(any(org.springframework.http.RequestEntity.class),
                any(ParameterizedTypeReference.class)))
                .willReturn(new ResponseEntity<>(attributes, HttpStatus.OK));

        given(userRepository.findByKakaoId("123456789")).willReturn(Optional.empty());
        given(userRepository.save(any(User.class))).willAnswer(invocation -> {
            User user = invocation.getArgument(0);
            return User.builder()
                    .userId(2)
                    .email(user.getEmail())
                    .kakaoId(user.getKakaoId())
                    .username(user.getUsername())
                    .role(user.getRole())
                    .build();
        });

        // when
        OAuth2User result = customOAuth2UserService.loadUser(userRequest);

        // then
        assertThat(result).isInstanceOf(CustomOAuth2User.class);
        CustomOAuth2User customUser = (CustomOAuth2User) result;
        assertThat(customUser.getUser().getUserId()).isEqualTo(2);
        assertThat(customUser.getUser().getEmail()).isEqualTo("test@example.com");
        verify(userRepository).save(any(User.class));
    }
}
