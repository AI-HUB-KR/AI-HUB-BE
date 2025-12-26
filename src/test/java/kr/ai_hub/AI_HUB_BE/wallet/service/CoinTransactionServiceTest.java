package kr.ai_hub.AI_HUB_BE.wallet.service;

import kr.ai_hub.AI_HUB_BE.wallet.dto.CoinTransactionResponse;
import kr.ai_hub.AI_HUB_BE.wallet.domain.CoinTransaction;
import kr.ai_hub.AI_HUB_BE.wallet.domain.CoinTransactionRepository;
import kr.ai_hub.AI_HUB_BE.user.domain.User;
import kr.ai_hub.AI_HUB_BE.user.domain.UserRepository;
import kr.ai_hub.AI_HUB_BE.global.security.SecurityContextHelper;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CoinTransactionServiceTest {

    @InjectMocks
    private CoinTransactionService coinTransactionService;

    @Mock
    private CoinTransactionRepository coinTransactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityContextHelper securityContextHelper;

    @Test
    @DisplayName("코인 거래 내역 조회 - 필터 없음")
    void getTransactions_NoFilter_Success() {
        // given
        Integer userId = 1;
        User user = User.builder().build();
        Pageable pageable = PageRequest.of(0, 10);

        CoinTransaction transaction = CoinTransaction.builder()
                .user(user)
                .coinUsage(BigDecimal.valueOf(100))
                .balanceAfter(BigDecimal.valueOf(100))
                .transactionType("CHARGE")
                .description("Test")
                .build();
        // Set createdAt using reflection
        try {
            java.lang.reflect.Field createdAtField = CoinTransaction.class.getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(transaction, LocalDateTime.now());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Page<CoinTransaction> transactionPage = new PageImpl<>(List.of(transaction));

        given(securityContextHelper.getCurrentUserId()).willReturn(userId);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(coinTransactionRepository.findByUser(user, pageable)).willReturn(transactionPage);

        // when
        Page<CoinTransactionResponse> result = coinTransactionService.getTransactions(
                null, null, null, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        verify(coinTransactionRepository).findByUser(user, pageable);
    }

    @Test
    @DisplayName("코인 거래 내역 조회 - 거래 타입 필터만")
    void getTransactions_TypeFilterOnly_Success() {
        // given
        Integer userId = 1;
        User user = User.builder().build();
        String transactionType = "CHARGE";
        Pageable pageable = PageRequest.of(0, 10);

        CoinTransaction transaction = CoinTransaction.builder()
                .user(user)
                .coinUsage(BigDecimal.valueOf(100))
                .balanceAfter(BigDecimal.valueOf(100))
                .transactionType("CHARGE")
                .description("Test")
                .build();
        try {
            java.lang.reflect.Field createdAtField = CoinTransaction.class.getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(transaction, LocalDateTime.now());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Page<CoinTransaction> transactionPage = new PageImpl<>(List.of(transaction));

        given(securityContextHelper.getCurrentUserId()).willReturn(userId);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(coinTransactionRepository.findByUserAndTransactionType(user, transactionType, pageable))
                .willReturn(transactionPage);

        // when
        Page<CoinTransactionResponse> result = coinTransactionService.getTransactions(
                transactionType, null, null, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).transactionType()).isEqualTo("CHARGE");
        verify(coinTransactionRepository).findByUserAndTransactionType(user, transactionType, pageable);
    }

    @Test
    @DisplayName("코인 거래 내역 조회 - 날짜 범위 필터만")
    void getTransactions_DateFilterOnly_Success() {
        // given
        Integer userId = 1;
        User user = User.builder().build();
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 12, 31);
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        Pageable pageable = PageRequest.of(0, 10);

        CoinTransaction transaction = CoinTransaction.builder()
                .user(user)
                .coinUsage(BigDecimal.valueOf(100))
                .balanceAfter(BigDecimal.valueOf(100))
                .transactionType("CHARGE")
                .description("Test")
                .build();
        try {
            java.lang.reflect.Field createdAtField = CoinTransaction.class.getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(transaction, LocalDateTime.of(2024, 6, 15, 12, 0));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Page<CoinTransaction> transactionPage = new PageImpl<>(List.of(transaction));

        given(securityContextHelper.getCurrentUserId()).willReturn(userId);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(coinTransactionRepository.findByUserAndCreatedAtBetween(
                eq(user), eq(startDateTime), eq(endDateTime), eq(pageable)))
                .willReturn(transactionPage);

        // when
        Page<CoinTransactionResponse> result = coinTransactionService.getTransactions(
                null, startDate, endDate, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        verify(coinTransactionRepository).findByUserAndCreatedAtBetween(
                user, startDateTime, endDateTime, pageable);
    }

    @Test
    @DisplayName("코인 거래 내역 조회 - 타입 + 날짜 범위 필터")
    void getTransactions_TypeAndDateFilter_Success() {
        // given
        Integer userId = 1;
        User user = User.builder().build();
        String transactionType = "USAGE";
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 12, 31);
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        Pageable pageable = PageRequest.of(0, 10);

        CoinTransaction transaction = CoinTransaction.builder()
                .user(user)
                .coinUsage(BigDecimal.valueOf(-50))
                .balanceAfter(BigDecimal.valueOf(50))
                .transactionType("USAGE")
                .description("Test")
                .build();
        try {
            java.lang.reflect.Field createdAtField = CoinTransaction.class.getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(transaction, LocalDateTime.of(2024, 6, 15, 12, 0));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Page<CoinTransaction> transactionPage = new PageImpl<>(List.of(transaction));

        given(securityContextHelper.getCurrentUserId()).willReturn(userId);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(coinTransactionRepository.findByUserAndTransactionTypeAndCreatedAtBetween(
                eq(user), eq(transactionType), eq(startDateTime), eq(endDateTime), eq(pageable)))
                .willReturn(transactionPage);

        // when
        Page<CoinTransactionResponse> result = coinTransactionService.getTransactions(
                transactionType, startDate, endDate, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).transactionType()).isEqualTo("USAGE");
        verify(coinTransactionRepository).findByUserAndTransactionTypeAndCreatedAtBetween(
                user, transactionType, startDateTime, endDateTime, pageable);
    }

    @Test
    @DisplayName("코인 거래 내역 조회 - 사용자 없음")
    void getTransactions_UserNotFound() {
        // given
        Integer userId = 1;
        Pageable pageable = PageRequest.of(0, 10);

        given(securityContextHelper.getCurrentUserId()).willReturn(userId);
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> coinTransactionService.getTransactions(null, null, null, pageable))
                .isInstanceOf(UserNotFoundException.class);
    }
}
