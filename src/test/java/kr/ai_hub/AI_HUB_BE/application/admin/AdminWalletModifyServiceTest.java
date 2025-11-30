package kr.ai_hub.AI_HUB_BE.application.admin;

import kr.ai_hub.AI_HUB_BE.domain.user.UserWallet;
import kr.ai_hub.AI_HUB_BE.domain.user.UserWalletRepository;
import kr.ai_hub.AI_HUB_BE.global.error.exception.WalletNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminWalletModifyServiceTest {

    @InjectMocks
    private AdminWalletModifyService adminWalletModifyService;

    @Mock
    private UserWalletRepository userWalletRepository;

    @Test
    @DisplayName("사용자 지갑 잔액 설정 - 증가")
    void setUserBalance_Increase() {
        // given
        Integer userId = 1;
        BigDecimal currentBalance = BigDecimal.valueOf(100);
        BigDecimal newBalance = BigDecimal.valueOf(200);

        UserWallet wallet = UserWallet.builder()
                .balance(currentBalance)
                .build();

        given(userWalletRepository.findByUserUserId(userId)).willReturn(Optional.of(wallet));

        // when
        adminWalletModifyService.setUserBalance(userId, newBalance);

        // then
        assertThat(wallet.getBalance()).isEqualByComparingTo(newBalance);
        verify(userWalletRepository).findByUserUserId(userId);
    }

    @Test
    @DisplayName("사용자 지갑 잔액 설정 - 감소")
    void setUserBalance_Decrease() {
        // given
        Integer userId = 1;
        BigDecimal currentBalance = BigDecimal.valueOf(200);
        BigDecimal newBalance = BigDecimal.valueOf(100);

        UserWallet wallet = UserWallet.builder()
                .balance(currentBalance)
                .build();

        given(userWalletRepository.findByUserUserId(userId)).willReturn(Optional.of(wallet));

        // when
        adminWalletModifyService.setUserBalance(userId, newBalance);

        // then
        assertThat(wallet.getBalance()).isEqualByComparingTo(newBalance);
        verify(userWalletRepository).findByUserUserId(userId);
    }

    @Test
    @DisplayName("사용자 지갑 잔액 설정 - 동일 금액")
    void setUserBalance_SameAmount() {
        // given
        Integer userId = 1;
        BigDecimal currentBalance = BigDecimal.valueOf(100);
        BigDecimal newBalance = BigDecimal.valueOf(100);

        UserWallet wallet = UserWallet.builder()
                .balance(currentBalance)
                .build();

        given(userWalletRepository.findByUserUserId(userId)).willReturn(Optional.of(wallet));

        // when
        adminWalletModifyService.setUserBalance(userId, newBalance);

        // then
        assertThat(wallet.getBalance()).isEqualByComparingTo(newBalance);
        verify(userWalletRepository).findByUserUserId(userId);
    }

    @Test
    @DisplayName("사용자 지갑 잔액 설정 - 지갑 없음")
    void setUserBalance_WalletNotFound() {
        // given
        Integer userId = 999;
        BigDecimal newBalance = BigDecimal.valueOf(100);

        given(userWalletRepository.findByUserUserId(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminWalletModifyService.setUserBalance(userId, newBalance))
                .isInstanceOf(WalletNotFoundException.class)
                .hasMessageContaining("지갑을 찾을 수 없습니다");
    }
}
