package kr.ai_hub.AI_HUB_BE.application.userwallet;

import kr.ai_hub.AI_HUB_BE.application.userwallet.dto.BalanceResponse;
import kr.ai_hub.AI_HUB_BE.application.userwallet.dto.UserWalletResponse;
import kr.ai_hub.AI_HUB_BE.domain.user.UserWallet;
import kr.ai_hub.AI_HUB_BE.domain.user.UserWalletRepository;
import kr.ai_hub.AI_HUB_BE.global.auth.SecurityContextHelper;
import kr.ai_hub.AI_HUB_BE.global.error.exception.WalletNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserWalletService {

    private final UserWalletRepository userWalletRepository;
    private final SecurityContextHelper securityContextHelper;

    /**
     * 현재 사용자의 지갑 상세 정보를 조회합니다.
     */
    public UserWalletResponse getUserWallet() {
        Integer userId = securityContextHelper.getCurrentUserId();
        log.debug("사용자 {} 지갑 조회", userId);

        UserWallet wallet = userWalletRepository.findByUserUserId(userId)
                .orElseThrow(() -> new WalletNotFoundException("지갑 정보를 찾을 수 없습니다: " + userId));

        return UserWalletResponse.from(wallet);
    }

    /**
     * 현재 사용자의 잔액만 조회합니다.
     */
    public BalanceResponse getBalance() {
        Integer userId = securityContextHelper.getCurrentUserId();
        log.debug("사용자 {} 잔액 조회", userId);

        UserWallet wallet = userWalletRepository.findByUserUserId(userId)
                .orElseThrow(() -> new WalletNotFoundException("지갑 정보를 찾을 수 없습니다: " + userId));

        return BalanceResponse.builder()
                .balance(wallet.getBalance())
                .paidBalance(wallet.getPaidBalance())
                .promotionBalance(wallet.getPromotionBalance())
                .build();
    }
}
