package kr.ai_hub.AI_HUB_BE.application.admin;

import kr.ai_hub.AI_HUB_BE.domain.user.UserWallet;
import kr.ai_hub.AI_HUB_BE.domain.user.UserWalletRepository;
import kr.ai_hub.AI_HUB_BE.global.error.exception.WalletNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminWalletModifyService {
    private final UserWalletRepository userWalletRepository;

      /**
       * 사용자 지갑 잔액을 직접 설정합니다 (관리자 전용)
       */
      @Transactional
      @PreAuthorize("hasRole('ADMIN')")
      public void setUserBalance(Integer userId, BigDecimal newBalance) {
          log.warn("관리자 지갑 직접 설정 - userId: {}, newBalance: {}", userId, newBalance);

          UserWallet wallet = userWalletRepository.findByUserUserId(userId)
              .orElseThrow(() -> new WalletNotFoundException("지갑을 찾을 수 없습니다: " + userId));

          // 직접 설정하려면 UserWallet 엔티티에 setter 또는 전용 메서드 필요
          // 현재는 addBalance/deductBalance로 조정하는 방식

          BigDecimal currentBalance = wallet.getBalance();
          BigDecimal difference = newBalance.subtract(currentBalance);

          if (difference.compareTo(BigDecimal.ZERO) > 0) {
              wallet.addBalance(difference);  // 증가
          } else if (difference.compareTo(BigDecimal.ZERO) < 0) {
              wallet.deductBalance(difference.abs());  // 감소
          }

          log.info("지갑 설정 완료 - userId: {}, 이전: {}, 현재: {}",
                   userId, currentBalance, wallet.getBalance());
      }
}
