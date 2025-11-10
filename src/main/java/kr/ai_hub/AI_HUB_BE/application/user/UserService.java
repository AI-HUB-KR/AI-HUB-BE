package kr.ai_hub.AI_HUB_BE.application.user;

import kr.ai_hub.AI_HUB_BE.application.user.dto.UpdateUserRequest;
import kr.ai_hub.AI_HUB_BE.application.user.dto.UserResponse;
import kr.ai_hub.AI_HUB_BE.domain.user.entity.User;
import kr.ai_hub.AI_HUB_BE.domain.user.repository.UserRepository;
import kr.ai_hub.AI_HUB_BE.global.error.exception.UserNotFoundException;
import kr.ai_hub.AI_HUB_BE.global.error.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    /**
     * 현재 인증된 사용자의 정보를 조회합니다.
     */
    public UserResponse getCurrentUser() {
        Integer userId = getCurrentUserId();
        log.debug("사용자 {} 정보 조회", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + userId));

        return UserResponse.from(user);
    }

    /**
     * 현재 인증된 사용자의 정보를 수정합니다.
     */
    @Transactional
    public UserResponse updateCurrentUser(UpdateUserRequest request) {
        Integer userId = getCurrentUserId();
        log.info("사용자 {} 정보 수정 요청", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + userId));

        // 이메일 중복 검사 (자신의 이메일은 제외)
        if (!user.getEmail().equals(request.email())) {
            if (userRepository.existsByEmail(request.email())) {
                log.warn("사용자 {} 이메일 중복: {}", userId, request.email());
                throw new ValidationException("이미 사용 중인 이메일입니다");
            }
        }

        // 사용자명 중복 검사 (자신의 사용자명은 제외)
        if (!user.getUsername().equals(request.username())) {
            if (userRepository.existsByUsername(request.username())) {
                log.warn("사용자 {} 사용자명 중복: {}", userId, request.username());
                throw new ValidationException("이미 사용 중인 사용자명입니다");
            }
        }

        user.update(request.username(), request.email());
        log.info("사용자 {} 정보 수정 완료", userId);

        return UserResponse.from(user);
    }

    /**
     * 현재 인증된 사용자를 소프트 삭제합니다.
     */
    @Transactional
    public void deleteCurrentUser() {
        Integer userId = getCurrentUserId();
        log.info("사용자 {} 탈퇴 요청", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + userId));

        user.softDelete();
        log.info("사용자 {} 탈퇴 완료", userId);
    }

    /**
     * SecurityContext에서 현재 인증된 사용자의 ID를 가져옵니다.
     */
    private Integer getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UserNotFoundException("인증되지 않은 사용자입니다");
        }

        try {
            return Integer.parseInt(authentication.getName());
        } catch (NumberFormatException e) {
            log.error("유효하지 않은 사용자 ID 형식: {}", authentication.getName());
            throw new UserNotFoundException("유효하지 않은 사용자 ID입니다");
        }
    }
}
