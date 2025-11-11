package kr.ai_hub.AI_HUB_BE.application.userwallet;

import kr.ai_hub.AI_HUB_BE.application.userwallet.dto.BalanceResponse;
import kr.ai_hub.AI_HUB_BE.application.userwallet.dto.UserWalletResponse;
import kr.ai_hub.AI_HUB_BE.domain.userwallet.entity.UserWallet;
import kr.ai_hub.AI_HUB_BE.domain.userwallet.repository.UserWalletRepository;
import kr.ai_hub.AI_HUB_BE.global.error.exception.UserNotFoundException;
import kr.ai_hub.AI_HUB_BE.global.error.exception.WalletNotFoundException;
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
public class UserWalletService {

    private final UserWalletRepository userWalletRepository;

    /**
     * 현재 사용자의 지갑 상세 정보를 조회합니다.
     */
    public UserWalletResponse getUserWallet() {
        Integer userId = getCurrentUserId();
        log.debug("사용자 {} 지갑 조회", userId);

        UserWallet wallet = userWalletRepository.findByUserUserId(userId)
                .orElseThrow(() -> new WalletNotFoundException("지갑 정보를 찾을 수 없습니다: " + userId));

        return UserWalletResponse.from(wallet);
    }

    /**
     * 현재 사용자의 잔액만 조회합니다.
     */
    public BalanceResponse getBalance() {
        Integer userId = getCurrentUserId();
        log.debug("사용자 {} 잔액 조회", userId);

        UserWallet wallet = userWalletRepository.findByUserUserId(userId)
                .orElseThrow(() -> new WalletNotFoundException("지갑 정보를 찾을 수 없습니다: " + userId));

        return BalanceResponse.builder()
                .balance(wallet.getBalance())
                .build();
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
