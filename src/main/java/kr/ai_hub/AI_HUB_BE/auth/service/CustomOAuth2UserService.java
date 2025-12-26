package kr.ai_hub.AI_HUB_BE.auth.service;

import kr.ai_hub.AI_HUB_BE.user.domain.User;
import kr.ai_hub.AI_HUB_BE.user.domain.UserRole;
import kr.ai_hub.AI_HUB_BE.user.domain.UserRepository;
import kr.ai_hub.AI_HUB_BE.global.security.oauth2.CustomOAuth2User;
import kr.ai_hub.AI_HUB_BE.global.security.oauth2.OAuth2UserInfo;
import kr.ai_hub.AI_HUB_BE.global.security.oauth2.OAuth2UserInfoFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        log.info("OAuth2 로그인 시도");

        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        log.debug("OAuth2 제공자: {}", registrationId);

        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(
                registrationId,
                oAuth2User.getAttributes()
        );

        User user = userRepository.findByKakaoId(userInfo.getId())
            .orElseGet(() -> {
                log.info("신규 사용자 감지, 회원가입 진행 - kakaoId: {}", userInfo.getId());
                return createUser(userInfo);
            });

        log.info("사용자 {} OAuth2 로그인 성공", user.getUserId());

        return new CustomOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                oAuth2User.getAttributes(),
                "id",
                user
        );
    }

    private User createUser(OAuth2UserInfo userInfo) {
        log.debug("신규 사용자 생성 중 - 이메일: {}", userInfo.getEmail());

        User user = User.builder()
                .kakaoId(userInfo.getId())
                .email(userInfo.getEmail())
                .username(userInfo.getName())
                .profileImageUrl(userInfo.getProfileImageUrl())
                .role(UserRole.ROLE_USER)
                .build();

        User savedUser = userRepository.save(user);
        log.info("사용자 {} 생성 완료", savedUser.getUserId());

        return savedUser;
    }
}