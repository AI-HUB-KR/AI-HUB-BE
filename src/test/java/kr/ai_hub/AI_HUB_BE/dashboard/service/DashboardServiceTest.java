package kr.ai_hub.AI_HUB_BE.dashboard.service;

import kr.ai_hub.AI_HUB_BE.dashboard.dto.ModelPricingResponse;
import kr.ai_hub.AI_HUB_BE.dashboard.dto.MonthlyUsageResponse;
import kr.ai_hub.AI_HUB_BE.dashboard.dto.UserStatsResponse;
import kr.ai_hub.AI_HUB_BE.aimodel.domain.AIModel;
import kr.ai_hub.AI_HUB_BE.aimodel.domain.AIModelRepository;
import kr.ai_hub.AI_HUB_BE.chat.domain.ChatRoomRepository;
import kr.ai_hub.AI_HUB_BE.chat.domain.MessageRepository;
import kr.ai_hub.AI_HUB_BE.wallet.domain.CoinTransaction;
import kr.ai_hub.AI_HUB_BE.wallet.domain.CoinTransactionRepository;
import kr.ai_hub.AI_HUB_BE.user.domain.User;
import kr.ai_hub.AI_HUB_BE.user.domain.UserRepository;
import kr.ai_hub.AI_HUB_BE.wallet.domain.UserWallet;
import kr.ai_hub.AI_HUB_BE.wallet.domain.UserWalletRepository;
import kr.ai_hub.AI_HUB_BE.global.security.SecurityContextHelper;
import kr.ai_hub.AI_HUB_BE.global.error.exception.UserNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @InjectMocks
    private DashboardService dashboardService;

    @Mock
    private AIModelRepository aiModelRepository;

    @Mock
    private CoinTransactionRepository coinTransactionRepository;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserWalletRepository userWalletRepository;

    @Mock
    private SecurityContextHelper securityContextHelper;

    @Test
    @DisplayName("모델 가격 조회 - 성공")
    void getModelPricing_Success() {
        // given
        AIModel model1 = AIModel.builder()
                .modelId(1)
                .modelName("gpt-4")
                .displayName("GPT-4")
                .inputPricePer1m(BigDecimal.valueOf(0.03))
                .outputPricePer1m(BigDecimal.valueOf(0.06))
                .isActive(true)
                .build();

        try {
            java.lang.reflect.Field createdAtField = AIModel.class.getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(model1, LocalDateTime.now());

            java.lang.reflect.Field updatedAtField = AIModel.class.getDeclaredField("updatedAt");
            updatedAtField.setAccessible(true);
            updatedAtField.set(model1, LocalDateTime.now());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        given(aiModelRepository.findByIsActiveTrueOrderByCreatedAtDesc())
                .willReturn(List.of(model1));

        // when
        List<ModelPricingResponse> result = dashboardService.getModelPricing();

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).modelName()).isEqualTo("gpt-4");
    }

    @Test
    @DisplayName("월별 사용량 조회 - 성공")
    void getMonthlyUsage_Success() {
        // given
        Integer userId = 1;
        User user = User.builder().build();

        given(securityContextHelper.getCurrentUserId()).willReturn(userId);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(coinTransactionRepository.findByUserAndCreatedAtBetween(any(), any(), any()))
                .willReturn(List.of());

        // when
        MonthlyUsageResponse result = dashboardService.getMonthlyUsage(2024, 1);

        // then
        assertThat(result.year()).isEqualTo(2024);
        assertThat(result.month()).isEqualTo(1);
        assertThat(result.totalCoinUsed()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("월별 사용량 조회 - 사용자 없음")
    void getMonthlyUsage_UserNotFound() {
        // given
        Integer userId = 1;

        given(securityContextHelper.getCurrentUserId()).willReturn(userId);
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> dashboardService.getMonthlyUsage(2024, 1))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("사용자 통계 조회 - 성공")
    void getUserStats_Success() {
        // given
        Integer userId = 1;
        User user = User.builder().build();

        try {
            java.lang.reflect.Field createdAtField = User.class.getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(user, LocalDateTime.now());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        UserWallet wallet = UserWallet.builder()
                .totalPurchased(BigDecimal.valueOf(1000))
                .totalUsed(BigDecimal.valueOf(500))
                .balance(BigDecimal.valueOf(500))
                .build();

        given(securityContextHelper.getCurrentUserId()).willReturn(userId);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userWalletRepository.findByUser(user)).willReturn(Optional.of(wallet));
        given(messageRepository.countByUser(user)).willReturn(100L);
        given(chatRoomRepository.countByUser(user)).willReturn(10L);
        given(coinTransactionRepository.findByUserAndTransactionType(user, "usage"))
                .willReturn(List.of());
        given(coinTransactionRepository.findByUserAndCreatedAtBetween(any(), any(), any()))
                .willReturn(List.of());

        // when
        UserStatsResponse result = dashboardService.getUserStats();

        // then
        assertThat(result.totalCoinPurchased()).isEqualByComparingTo(BigDecimal.valueOf(1000));
        assertThat(result.currentBalance()).isEqualByComparingTo(BigDecimal.valueOf(500));
        assertThat(result.totalMessages()).isEqualTo(100L);
        assertThat(result.totalChatRooms()).isEqualTo(10L);
    }
}
