package kr.ai_hub.AI_HUB_BE.wallet.service;

import kr.ai_hub.AI_HUB_BE.wallet.dto.PaymentResponse;
import kr.ai_hub.AI_HUB_BE.wallet.domain.WalletHistory;
import kr.ai_hub.AI_HUB_BE.wallet.domain.WalletHistoryRepository;
import kr.ai_hub.AI_HUB_BE.user.domain.User;
import kr.ai_hub.AI_HUB_BE.user.domain.UserRepository;
import kr.ai_hub.AI_HUB_BE.global.security.SecurityContextHelper;
import kr.ai_hub.AI_HUB_BE.global.error.exception.ForbiddenException;
import kr.ai_hub.AI_HUB_BE.global.error.exception.PaymentNotFoundException;
import kr.ai_hub.AI_HUB_BE.global.error.exception.UserNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WalletHistoryServiceTest {

    @InjectMocks
    private WalletHistoryService walletHistoryService;

    @Mock
    private WalletHistoryRepository walletHistoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityContextHelper securityContextHelper;

    @Test
    @DisplayName("결제 내역 조회 - 전체 조회 성공")
    void getPayments_Success() {
        // given
        Integer userId = 1;
        User user = User.builder().build();
        Pageable pageable = PageRequest.of(0, 10);

        WalletHistory history = WalletHistory.builder()
                .historyId(1L)
                .user(user)
                .payAmountKrw(BigDecimal.valueOf(10000))
                .paidCoin(BigDecimal.valueOf(100))
                .status("COMPLETED")
                .paymentMethod("CARD")
                .transactionId("tx_123")
                .createdAt(java.time.LocalDateTime.now())
                .build();
        Page<WalletHistory> historyPage = new PageImpl<>(List.of(history));

        given(securityContextHelper.getCurrentUserId()).willReturn(userId);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(walletHistoryRepository.findByUser(user, pageable)).willReturn(historyPage);

        // when
        Page<PaymentResponse> result = walletHistoryService.getPayments(null, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).historyId()).isEqualTo(1L);
        verify(walletHistoryRepository).findByUser(user, pageable);
    }

    @Test
    @DisplayName("결제 내역 조회 - 상태 필터링 성공")
    void getPayments_WithStatus_Success() {
        // given
        Integer userId = 1;
        User user = User.builder().build();
        String status = "COMPLETED";
        Pageable pageable = PageRequest.of(0, 10);

        WalletHistory history = WalletHistory.builder()
                .historyId(1L)
                .user(user)
                .payAmountKrw(BigDecimal.valueOf(10000))
                .paidCoin(BigDecimal.valueOf(100))
                .status("COMPLETED")
                .paymentMethod("CARD")
                .transactionId("tx_123")
                .createdAt(java.time.LocalDateTime.now())
                .build();
        Page<WalletHistory> historyPage = new PageImpl<>(List.of(history));

        given(securityContextHelper.getCurrentUserId()).willReturn(userId);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(walletHistoryRepository.findByUserAndStatus(user, status, pageable)).willReturn(historyPage);

        // when
        Page<PaymentResponse> result = walletHistoryService.getPayments(status, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).status()).isEqualTo("COMPLETED");
        verify(walletHistoryRepository).findByUserAndStatus(user, status, pageable);
    }

    @Test
    @DisplayName("결제 내역 조회 - 사용자 없음")
    void getPayments_UserNotFound() {
        // given
        Integer userId = 1;
        Pageable pageable = PageRequest.of(0, 10);

        given(securityContextHelper.getCurrentUserId()).willReturn(userId);
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> walletHistoryService.getPayments(null, pageable))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("결제 상세 조회 - 성공")
    void getPayment_Success() {
        // given
        Integer userId = 1;
        Long historyId = 1L;

        User user = User.builder().build();
        try {
            java.lang.reflect.Field idField = User.class.getDeclaredField("userId");
            idField.setAccessible(true);
            idField.set(user, userId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        WalletHistory history = WalletHistory.builder()
                .historyId(historyId)
                .user(user)
                .payAmountKrw(BigDecimal.valueOf(10000))
                .paidCoin(BigDecimal.valueOf(100))
                .status("COMPLETED")
                .paymentMethod("CARD")
                .transactionId("tx_123")
                .createdAt(java.time.LocalDateTime.now())
                .build();

        given(securityContextHelper.getCurrentUserId()).willReturn(userId);
        given(walletHistoryRepository.findById(historyId)).willReturn(Optional.of(history));

        // when
        PaymentResponse result = walletHistoryService.getPayment(historyId);

        // then
        assertThat(result.historyId()).isEqualTo(historyId);
    }

    @Test
    @DisplayName("결제 상세 조회 - 결제 내역 없음")
    void getPayment_NotFound() {
        // given
        Integer userId = 1;
        Long historyId = 1L;

        given(securityContextHelper.getCurrentUserId()).willReturn(userId);
        given(walletHistoryRepository.findById(historyId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> walletHistoryService.getPayment(historyId))
                .isInstanceOf(PaymentNotFoundException.class);
    }

    @Test
    @DisplayName("결제 상세 조회 - 권한 없음")
    void getPayment_Forbidden() {
        // given
        Integer userId = 1;
        Integer otherUserId = 2;
        Long historyId = 1L;

        User otherUser = User.builder().build();
        try {
            java.lang.reflect.Field idField = User.class.getDeclaredField("userId");
            idField.setAccessible(true);
            idField.set(otherUser, otherUserId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        WalletHistory history = WalletHistory.builder()
                .historyId(historyId)
                .user(otherUser)
                .payAmountKrw(BigDecimal.valueOf(10000))
                .paidCoin(BigDecimal.valueOf(100))
                .status("COMPLETED")
                .paymentMethod("CARD")
                .transactionId("tx_123")
                .createdAt(java.time.LocalDateTime.now())
                .build();

        given(securityContextHelper.getCurrentUserId()).willReturn(userId);
        given(walletHistoryRepository.findById(historyId)).willReturn(Optional.of(history));

        // when & then
        assertThatThrownBy(() -> walletHistoryService.getPayment(historyId))
                .isInstanceOf(ForbiddenException.class);
    }
}
