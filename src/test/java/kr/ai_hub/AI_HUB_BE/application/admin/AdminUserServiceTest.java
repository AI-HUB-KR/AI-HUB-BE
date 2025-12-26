package kr.ai_hub.AI_HUB_BE.application.admin;

import kr.ai_hub.AI_HUB_BE.controller.admin.dto.UserListResponse;
import kr.ai_hub.AI_HUB_BE.domain.payment.PaymentHistory;
import kr.ai_hub.AI_HUB_BE.domain.payment.PaymentHistoryRepository;
import kr.ai_hub.AI_HUB_BE.domain.payment.WalletHistoryType;
import kr.ai_hub.AI_HUB_BE.domain.user.User;
import kr.ai_hub.AI_HUB_BE.domain.user.UserRepository;
import kr.ai_hub.AI_HUB_BE.domain.user.UserRole;
import kr.ai_hub.AI_HUB_BE.domain.user.UserWallet;
import kr.ai_hub.AI_HUB_BE.domain.user.UserWalletRepository;
import kr.ai_hub.AI_HUB_BE.global.error.exception.ForbiddenException;
import kr.ai_hub.AI_HUB_BE.global.error.exception.UserNotFoundException;
import kr.ai_hub.AI_HUB_BE.global.error.exception.WalletNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @InjectMocks
    private AdminUserService adminUserService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserWalletRepository userWalletRepository;

    @Mock
    private PaymentHistoryRepository paymentHistoryRepository;

    @Test
    @DisplayName("사용자 권한 수정 - 성공")
    void modifyUserAuthority_Success() {
        // given
        Integer targetUserId = 1;
        Integer currentUserId = 2;
        UserRole newRole = UserRole.ROLE_ADMIN;

        User targetUser = User.builder()
                .userId(targetUserId)
                .username("testuser")
                .email("test@example.com")
                .role(UserRole.ROLE_USER)
                .build();

        given(userRepository.findById(targetUserId)).willReturn(Optional.of(targetUser));

        // when
        adminUserService.modifyUserAuthority(targetUserId, newRole, currentUserId);

        // then
        assertThat(targetUser.getRole()).isEqualTo(newRole);
        verify(userRepository).findById(targetUserId);
    }

    @Test
    @DisplayName("사용자 권한 수정 - 본인 권한 수정 시도")
    void modifyUserAuthority_SelfModification() {
        // given
        Integer targetUserId = 1;
        Integer currentUserId = 1;
        UserRole newRole = UserRole.ROLE_ADMIN;

        // when & then
        assertThatThrownBy(() -> adminUserService.modifyUserAuthority(targetUserId, newRole, currentUserId))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("본인의 권한은 수정할 수 없습니다");
    }

    @Test
    @DisplayName("사용자 권한 수정 - 사용자 없음")
    void modifyUserAuthority_UserNotFound() {
        // given
        Integer targetUserId = 999;
        Integer currentUserId = 1;
        UserRole newRole = UserRole.ROLE_ADMIN;

        given(userRepository.findById(targetUserId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminUserService.modifyUserAuthority(targetUserId, newRole, currentUserId))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("사용자를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("전체 사용자 정보 조회 - 성공")
    void getAllUsersWithWallet_Success() {
        // given
        UserWallet wallet1 = UserWallet.builder()
                .walletId(1)
                .balance(BigDecimal.valueOf(1000))
                .totalPurchased(BigDecimal.valueOf(2000))
                .totalUsed(BigDecimal.valueOf(1000))
                .build();

        UserWallet wallet2 = UserWallet.builder()
                .walletId(2)
                .balance(BigDecimal.valueOf(500))
                .totalPurchased(BigDecimal.valueOf(1000))
                .totalUsed(BigDecimal.valueOf(500))
                .build();

        User user1 = User.builder()
                .userId(1)
                .username("user1")
                .email("user1@example.com")
                .role(UserRole.ROLE_USER)
                .build();

        User user2 = User.builder()
                .userId(2)
                .username("user2")
                .email("user2@example.com")
                .role(UserRole.ROLE_ADMIN)
                .build();

        given(userRepository.findAll()).willReturn(List.of(user1, user2));

        // when
        List<UserListResponse> result = adminUserService.getAllUsersWithWallet();

        // then
        assertThat(result).hasSize(2);
        verify(userRepository).findAll();
    }

    @Test
    @DisplayName("프로모션 코인 수정 - 증가")
    void modifyPromotionBalance_Increase() {
        // given
        Integer userId = 1;
        Integer adminId = 2;
        BigDecimal promotionChange = BigDecimal.valueOf(100);  // 증가

        User user = User.builder()
                .userId(userId)
                .username("testuser")
                .email("test@example.com")
                .role(UserRole.ROLE_USER)
                .build();

        UserWallet wallet = UserWallet.builder()
                .user(user)
                .balance(BigDecimal.ZERO)
                .paidBalance(BigDecimal.ZERO)
                .promotionBalance(BigDecimal.ZERO)
                .totalPurchased(BigDecimal.ZERO)
                .totalUsed(BigDecimal.ZERO)
                .build();

        given(userWalletRepository.findByUserUserId(userId)).willReturn(Optional.of(wallet));

        // when
        adminUserService.modifyPromotionBalance(userId, promotionChange, adminId);

        // then
        assertThat(wallet.getPromotionBalance()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(wallet.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(100));
        verify(userWalletRepository).findByUserUserId(userId);
        verify(paymentHistoryRepository).save(any(PaymentHistory.class));
    }

    @Test
    @DisplayName("프로모션 코인 수정 - 감소")
    void modifyPromotionBalance_Decrease() {
        // given
        Integer userId = 1;
        Integer adminId = 2;
        BigDecimal promotionChange = BigDecimal.valueOf(-50);  // 감소

        User user = User.builder()
                .userId(userId)
                .username("testuser")
                .email("test@example.com")
                .role(UserRole.ROLE_USER)
                .build();

        UserWallet wallet = UserWallet.builder()
                .user(user)
                .balance(BigDecimal.valueOf(100))
                .paidBalance(BigDecimal.ZERO)
                .promotionBalance(BigDecimal.valueOf(100))
                .totalPurchased(BigDecimal.ZERO)
                .totalUsed(BigDecimal.ZERO)
                .build();

        given(userWalletRepository.findByUserUserId(userId)).willReturn(Optional.of(wallet));

        // when
        adminUserService.modifyPromotionBalance(userId, promotionChange, adminId);

        // then
        assertThat(wallet.getPromotionBalance()).isEqualByComparingTo(BigDecimal.valueOf(50));
        assertThat(wallet.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(50));
        verify(userWalletRepository).findByUserUserId(userId);
        verify(paymentHistoryRepository).save(any(PaymentHistory.class));
    }

    @Test
    @DisplayName("프로모션 코인 수정 - 0원 변경 시 처리 안함")
    void modifyPromotionBalance_ZeroChange() {
        // given
        Integer userId = 1;
        Integer adminId = 2;
        BigDecimal promotionChange = BigDecimal.ZERO;  // 0원 변경

        // when
        adminUserService.modifyPromotionBalance(userId, promotionChange, adminId);

        // then
        verify(userWalletRepository, org.mockito.Mockito.never()).findByUserUserId(any());
        verify(paymentHistoryRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    @DisplayName("프로모션 코인 수정 - 지갑 없음")
    void modifyPromotionBalance_WalletNotFound() {
        // given
        Integer userId = 999;
        Integer adminId = 2;
        BigDecimal promotionChange = BigDecimal.valueOf(100);

        given(userWalletRepository.findByUserUserId(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminUserService.modifyPromotionBalance(userId, promotionChange, adminId))
                .isInstanceOf(WalletNotFoundException.class)
                .hasMessageContaining("지갑을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("프로모션 코인 수정 - 잔액 부족")
    void modifyPromotionBalance_InsufficientBalance() {
        // given
        Integer userId = 1;
        Integer adminId = 2;
        BigDecimal promotionChange = BigDecimal.valueOf(-200);  // 현재 잔액보다 많이 감소

        User user = User.builder()
                .userId(userId)
                .username("testuser")
                .email("test@example.com")
                .role(UserRole.ROLE_USER)
                .build();

        UserWallet wallet = UserWallet.builder()
                .user(user)
                .balance(BigDecimal.valueOf(100))
                .paidBalance(BigDecimal.ZERO)
                .promotionBalance(BigDecimal.valueOf(100))  // 현재 100원
                .totalPurchased(BigDecimal.ZERO)
                .totalUsed(BigDecimal.ZERO)
                .build();

        given(userWalletRepository.findByUserUserId(userId)).willReturn(Optional.of(wallet));

        // when & then
        assertThatThrownBy(() -> adminUserService.modifyPromotionBalance(userId, promotionChange, adminId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("프로모션 코인이 부족합니다");
    }
}
