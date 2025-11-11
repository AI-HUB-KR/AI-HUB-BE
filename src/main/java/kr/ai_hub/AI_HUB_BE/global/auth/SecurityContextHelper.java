package kr.ai_hub.AI_HUB_BE.global.auth;

import kr.ai_hub.AI_HUB_BE.global.error.exception.UserNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Spring Security Context에서 현재 인증된 사용자 정보를 추출하는 헬퍼 클래스
 * 모든 서비스 레이어에서 공통으로 사용됩니다.
 */
@Component
@Slf4j
public class SecurityContextHelper {

    /**
     * 현재 인증된 사용자의 ID를 반환합니다.
     *
     * @return 현재 사용자의 ID
     * @throws UserNotFoundException 인증되지 않았거나 유효하지 않은 사용자 ID인 경우
     */
    public Integer getCurrentUserId() {
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
