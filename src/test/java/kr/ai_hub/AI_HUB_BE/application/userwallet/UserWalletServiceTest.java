package kr.ai_hub.AI_HUB_BE.application.userwallet;

import kr.ai_hub.AI_HUB_BE.application.userwallet.dto.BalanceResponse;
import kr.ai_hub.AI_HUB_BE.application.userwallet.dto.UserWalletResponse;
import kr.ai_hub.AI_HUB_BE.domain.user.User;
import kr.ai_hub.AI_HUB_BE.domain.user.UserWallet;
import kr.ai_hub.AI_HUB_BE.domain.user.UserWalletRepository;
import kr.ai_hub.AI_HUB_BE.global.auth.SecurityContextHelper;
import kr.ai_hub.AI_HUB_BE.global.error.exception.WalletNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UserWalletServiceTest {

    @InjectMocks
    private UserWalletService userWalletService;

    @Mock
    private UserWalletRepository userWalletRepository;

    @Mock
    private SecurityContextHelper securityContextHelper;

    @Test
    @DisplayName("지갑 상세 조회 - 성공")
    void getUserWallet_Success() {
        // given
        Integer userId = 1;
        User user = User.builder().userId(userId).build();
        UserWallet wallet = UserWallet.builder()
                .walletId(1)
                .user(user)
                .balance(BigDecimal.TEN)
                .totalPurchased(BigDecimal.ZERO)
                .totalUsed(BigDecimal.ZERO)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        given(securityContextHelper.getCurrentUserId()).willReturn(userId);
        given(userWalletRepository.findByUserUserId(userId)).willReturn(Optional.of(wallet));

        // when
        UserWalletResponse response = userWalletService.getUserWallet();

        // then
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.balance()).isEqualTo(BigDecimal.TEN);
    }

    @Test
    @DisplayName("지갑 상세 조회 - 실패 (지갑 없음)")
    void getUserWallet_NotFound() {
        // given
        Integer userId = 1;
        given(securityContextHelper.getCurrentUserId()).willReturn(userId);
        given(userWalletRepository.findByUserUserId(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userWalletService.getUserWallet())
                .isInstanceOf(WalletNotFoundException.class)
                .hasMessageContaining("지갑 정보를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("잔액 조회 - 성공")
    void getBalance_Success() {
        // given
        Integer userId = 1;
        UserWallet wallet = UserWallet.builder()
                .balance(BigDecimal.valueOf(100))
                .build();

        given(securityContextHelper.getCurrentUserId()).willReturn(userId);
        given(userWalletRepository.findByUserUserId(userId)).willReturn(Optional.of(wallet));

        // when
        BalanceResponse response = userWalletService.getBalance();

        // then
        assertThat(response.balance()).isEqualTo(BigDecimal.valueOf(100));
    }
}
