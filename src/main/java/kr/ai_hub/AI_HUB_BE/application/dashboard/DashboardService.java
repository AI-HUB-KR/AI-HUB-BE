package kr.ai_hub.AI_HUB_BE.application.dashboard;

import kr.ai_hub.AI_HUB_BE.application.dashboard.dto.*;
import kr.ai_hub.AI_HUB_BE.domain.aimodel.entity.AIModel;
import kr.ai_hub.AI_HUB_BE.domain.aimodel.repository.AIModelRepository;
import kr.ai_hub.AI_HUB_BE.domain.chatroom.repository.ChatRoomRepository;
import kr.ai_hub.AI_HUB_BE.domain.cointransaction.entity.CoinTransaction;
import kr.ai_hub.AI_HUB_BE.domain.cointransaction.repository.CoinTransactionRepository;
import kr.ai_hub.AI_HUB_BE.domain.message.repository.MessageRepository;
import kr.ai_hub.AI_HUB_BE.domain.user.entity.User;
import kr.ai_hub.AI_HUB_BE.domain.user.repository.UserRepository;
import kr.ai_hub.AI_HUB_BE.domain.userwallet.entity.UserWallet;
import kr.ai_hub.AI_HUB_BE.domain.userwallet.repository.UserWalletRepository;
import kr.ai_hub.AI_HUB_BE.global.auth.SecurityContextHelper;
import kr.ai_hub.AI_HUB_BE.global.error.exception.UserNotFoundException;
import kr.ai_hub.AI_HUB_BE.global.error.exception.WalletNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final AIModelRepository aiModelRepository;
    private final CoinTransactionRepository coinTransactionRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final UserWalletRepository userWalletRepository;
    private final SecurityContextHelper securityContextHelper;

    /**
     * 모든 활성화된 AI 모델의 가격 정보를 조회합니다 (Public API).
     */
    public List<ModelPricingResponse> getModelPricing() {
        log.debug("모델 가격 대시보드 조회");

        return aiModelRepository.findByIsActiveTrueOrderByCreatedAtDesc()
                .stream()
                .map(ModelPricingResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 현재 사용자의 월별 모델별 코인 사용량 통계를 조회합니다.
     */
    public MonthlyUsageResponse getMonthlyUsage(Integer year, Integer month) {
        Integer userId = securityContextHelper.getCurrentUserId();
        log.debug("사용자 {} 월별 사용량 조회: {}/{}", userId, year, month);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + userId));

        // 해당 월의 시작일과 종료일 계산
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        // 해당 월의 usage 타입 거래만 조회
        List<CoinTransaction> transactions = coinTransactionRepository.findByUserAndCreatedAtBetween(
                user, startDateTime, endDateTime);

        // usage 타입만 필터링
        List<CoinTransaction> usageTransactions = transactions.stream()
                .filter(t -> "usage".equalsIgnoreCase(t.getTransactionType()))
                .collect(Collectors.toList());

        // 전체 코인 사용량 계산 (음수이므로 절댓값)
        BigDecimal totalCoinUsed = usageTransactions.stream()
                .map(CoinTransaction::getAmount)
                .map(BigDecimal::abs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 모델별 집계
        Map<AIModel, List<CoinTransaction>> groupedByModel = usageTransactions.stream()
                .filter(t -> t.getAiModel() != null)
                .collect(Collectors.groupingBy(CoinTransaction::getAiModel));

        List<ModelUsageDetail> modelUsage = groupedByModel.entrySet().stream()
                .map(entry -> {
                    AIModel model = entry.getKey();
                    List<CoinTransaction> modelTransactions = entry.getValue();

                    BigDecimal coinUsed = modelTransactions.stream()
                            .map(CoinTransaction::getAmount)
                            .map(BigDecimal::abs)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    long messageCount = modelTransactions.size();

                    BigDecimal tokenCount = modelTransactions.stream()
                            .map(CoinTransaction::getAmount)
                            .map(BigDecimal::abs)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    double percentage = totalCoinUsed.compareTo(BigDecimal.ZERO) > 0
                            ? coinUsed.divide(totalCoinUsed, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .doubleValue()
                            : 0.0;

                    return ModelUsageDetail.builder()
                            .modelId(model.getModelId())
                            .modelName(model.getModelName())
                            .displayName(model.getDisplayName())
                            .coinUsed(coinUsed)
                            .messageCount(messageCount)
                            .tokenCount(tokenCount)
                            .percentage(percentage)
                            .build();
                })
                .sorted(Comparator.comparing(ModelUsageDetail::coinUsed).reversed())
                .collect(Collectors.toList());

        // 일별 집계
        Map<LocalDate, List<CoinTransaction>> groupedByDate = usageTransactions.stream()
                .collect(Collectors.groupingBy(t -> t.getCreatedAt().toLocalDate()));

        List<DailyUsageDetail> dailyUsage = groupedByDate.entrySet().stream()
                .map(entry -> {
                    LocalDate date = entry.getKey();
                    List<CoinTransaction> dayTransactions = entry.getValue();

                    BigDecimal coinUsed = dayTransactions.stream()
                            .map(CoinTransaction::getAmount)
                            .map(BigDecimal::abs)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    long messageCount = dayTransactions.size();

                    return DailyUsageDetail.builder()
                            .date(date)
                            .coinUsed(coinUsed)
                            .messageCount(messageCount)
                            .build();
                })
                .sorted(Comparator.comparing(DailyUsageDetail::date))
                .collect(Collectors.toList());

        return MonthlyUsageResponse.builder()
                .year(year)
                .month(month)
                .totalCoinUsed(totalCoinUsed)
                .modelUsage(modelUsage)
                .dailyUsage(dailyUsage)
                .build();
    }

    /**
     * 현재 사용자의 코인 및 활동 통계를 요약합니다.
     */
    public UserStatsResponse getUserStats() {
        Integer userId = securityContextHelper.getCurrentUserId();
        log.debug("사용자 {} 통계 요약 조회", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + userId));

        UserWallet wallet = userWalletRepository.findByUser(user)
                .orElseThrow(() -> new WalletNotFoundException("지갑 정보를 찾을 수 없습니다"));

        // 전체 메시지 수 (최적화된 COUNT 쿼리 사용)
        long totalMessages = messageRepository.countByUser(user);

        // 전체 채팅방 수 (최적화된 COUNT 쿼리 사용)
        long totalChatRooms = chatRoomRepository.countByUser(user);

        // 가장 많이 사용한 모델 계산
        List<CoinTransaction> allUsageTransactions = coinTransactionRepository.findByUserAndTransactionType(user, "usage");

        MostUsedModel mostUsedModel = null;
        if (!allUsageTransactions.isEmpty()) {
            Map<AIModel, BigDecimal> modelUsageMap = allUsageTransactions.stream()
                    .filter(t -> t.getAiModel() != null)
                    .collect(Collectors.groupingBy(
                            CoinTransaction::getAiModel,
                            Collectors.reducing(BigDecimal.ZERO,
                                    t -> t.getAmount().abs(),
                                    BigDecimal::add)
                    ));

            BigDecimal totalUsage = modelUsageMap.values().stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (totalUsage.compareTo(BigDecimal.ZERO) > 0) {
                Map.Entry<AIModel, BigDecimal> topEntry = modelUsageMap.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .orElse(null);

                if (topEntry != null) {
                    AIModel topModel = topEntry.getKey();
                    BigDecimal topModelUsage = topEntry.getValue();
                    double usagePercentage = topModelUsage.divide(totalUsage, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .doubleValue();

                    mostUsedModel = MostUsedModel.builder()
                            .modelId(topModel.getModelId())
                            .modelName(topModel.getModelName())
                            .displayName(topModel.getDisplayName())
                            .usagePercentage(usagePercentage)
                            .build();
                }
            }
        }

        // 최근 30일 사용량
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<CoinTransaction> last30DaysTransactions = coinTransactionRepository.findByUserAndCreatedAtBetween(
                user, thirtyDaysAgo, LocalDateTime.now());

        BigDecimal last30DaysUsage = last30DaysTransactions.stream()
                .filter(t -> "usage".equalsIgnoreCase(t.getTransactionType()))
                .map(CoinTransaction::getAmount)
                .map(BigDecimal::abs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return UserStatsResponse.builder()
                .totalCoinPurchased(wallet.getTotalPurchased())
                .totalCoinUsed(wallet.getTotalUsed())
                .currentBalance(wallet.getBalance())
                .totalMessages(totalMessages)
                .totalChatRooms(totalChatRooms)
                .mostUsedModel(mostUsedModel)
                .last30DaysUsage(last30DaysUsage)
                .memberSince(user.getCreatedAt().toInstant(ZoneOffset.UTC))
                .build();
    }
}
