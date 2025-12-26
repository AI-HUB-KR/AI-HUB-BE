package kr.ai_hub.AI_HUB_BE.admin.service;

import kr.ai_hub.AI_HUB_BE.admin.dto.UserListResponse;
import kr.ai_hub.AI_HUB_BE.wallet.domain.WalletHistory;
import kr.ai_hub.AI_HUB_BE.wallet.domain.WalletHistoryRepository;
import kr.ai_hub.AI_HUB_BE.wallet.domain.WalletHistoryType;
import kr.ai_hub.AI_HUB_BE.user.domain.User;
import kr.ai_hub.AI_HUB_BE.user.domain.UserRepository;
import kr.ai_hub.AI_HUB_BE.user.domain.UserRole;
import kr.ai_hub.AI_HUB_BE.wallet.domain.UserWallet;
import kr.ai_hub.AI_HUB_BE.wallet.domain.UserWalletRepository;
import kr.ai_hub.AI_HUB_BE.global.error.exception.ForbiddenException;
import kr.ai_hub.AI_HUB_BE.global.error.exception.UserNotFoundException;
import kr.ai_hub.AI_HUB_BE.global.error.exception.WalletNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserService {
    private final UserRepository userRepository;
    private final UserWalletRepository userWalletRepository;
    private final WalletHistoryRepository walletHistoryRepository;

    /**
     * 사용자 권한을 수정합니다 (관리자 전용)
     * 본인의 권한은 수정할 수 없습니다.
     *
     * @param targetUserId  권한을 수정할 대상 사용자 ID
     * @param newRole       새로운 권한
     * @param currentUserId 현재 요청을 보낸 관리자 ID
     * @throws UserNotFoundException 대상 사용자를 찾을 수 없는 경우
     * @throws ForbiddenException    본인의 권한을 수정하려고 시도한 경우
     */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void modifyUserAuthority(Integer targetUserId, UserRole newRole, Integer currentUserId) {
        log.info("사용자 권한 수정 요청 - targetUserId: {}, newRole: {}, currentUserId: {}",
                targetUserId, newRole, currentUserId);

        // 본인 권한 수정 방지 검증
        if (targetUserId.equals(currentUserId)) {
            log.warn("본인 권한 수정 시도 차단 - userId: {}", currentUserId);
            throw new ForbiddenException("본인의 권한은 수정할 수 없습니다");
        }

        // 대상 사용자 조회
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + targetUserId));

        UserRole oldRole = targetUser.getRole();

        // 권한 수정
        targetUser.updateRole(newRole);

        log.info("사용자 권한 수정 완료 - userId: {}, 이전 권한: {}, 새 권한: {}",
                targetUserId, oldRole, newRole);
    }

    /**
     * 전체 사용자 정보를 지갑 정보와 함께 조회합니다 (관리자 전용)
     * 본인을 포함한 모든 사용자 정보를 반환합니다.
     *
     * @return 사용자 정보 리스트
     */
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserListResponse> getAllUsersWithWallet() {
        log.info("전체 사용자 정보 조회 요청");

        List<User> users = userRepository.findAll();

        List<UserListResponse> response = users.stream()
                .map(user -> UserListResponse.from(user, user.getWallet()))
                .collect(Collectors.toList());

        log.info("전체 사용자 정보 조회 완료 - 사용자 수: {}", response.size());
        return response;
    }

    /**
     * 사용자 프로모션 코인 잔액을 수정합니다 (관리자 전용)
     * WalletHistory(wallet_history)에 변경 이력을 기록합니다.
     *
     * @param userId            잔액을 수정할 사용자 ID
     * @param promotionChange   프로모션 코인 변경량 (양수: 증가, 음수: 감소)
     * @param currentAdminId    현재 요청을 보낸 관리자 ID
     * @throws WalletNotFoundException 지갑을 찾을 수 없는 경우
     * @throws IllegalArgumentException 프로모션 코인이 0 미만으로 내려가는 경우
     */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void modifyPromotionBalance(Integer userId, BigDecimal promotionChange, Integer currentAdminId) {
        log.warn("관리자 프로모션 코인 수정 - userId: {}, change: {}, adminId: {}",
                userId, promotionChange, currentAdminId);

        // 변경량이 0이면 처리하지 않음
        if (promotionChange.compareTo(BigDecimal.ZERO) == 0) {
            log.info("변경량이 0이므로 처리하지 않습니다");
            return;
        }

        // 지갑 조회
        UserWallet wallet = userWalletRepository.findByUserUserId(userId)
                .orElseThrow(() -> new WalletNotFoundException("지갑을 찾을 수 없습니다: " + userId));

        BigDecimal currentPromotionBalance = wallet.getPromotionBalance();

        // 감소 시 잔액 검증
        if (promotionChange.compareTo(BigDecimal.ZERO) < 0) {
            BigDecimal newPromotionBalance = currentPromotionBalance.add(promotionChange);
            if (newPromotionBalance.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException(
                        String.format("프로모션 코인이 부족합니다. 현재: %s, 요청: %s",
                                currentPromotionBalance, promotionChange));
            }
        }

        // WalletHistoryType 결정
        WalletHistoryType historyType = promotionChange.compareTo(BigDecimal.ZERO) > 0
                ? WalletHistoryType.PROMOTION
                : WalletHistoryType.PROMOTION_RETRIEVE;

        // 프로모션 코인 변경
        if (promotionChange.compareTo(BigDecimal.ZERO) > 0) {
            wallet.addPromotionBalance(promotionChange);  // 증가
        } else {
            wallet.deductPromotionBalance(promotionChange.abs());  // 감소
        }

        // WalletHistory 기록 생성
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("adminId", currentAdminId);
        metadata.put("userId", userId);
        metadata.put("reason", "관리자에 의한 프로모션 코인 " + (promotionChange.compareTo(BigDecimal.ZERO) > 0 ? "지급" : "회수"));

        String transactionId = "admin_promo_" + UUID.randomUUID().toString();

        WalletHistory walletHistory = WalletHistory.builder()
                .user(wallet.getUser())
                .transactionId(transactionId)
                .paymentMethod("ADMIN_MODIFY")
                .payAmountKrw(BigDecimal.ZERO)
                .payAmountUsd(BigDecimal.ZERO)
                .paidCoin(BigDecimal.ZERO)
                .promotionCoin(promotionChange.abs())
                .status("completed")
                .completedAt(LocalDateTime.now())
                .walletHistoryType(historyType)
                .metadata(metadata)
                .build();

        walletHistoryRepository.save(walletHistory);

        log.info("프로모션 코인 수정 완료 - userId: {}, 이전: {}, 현재: {}, historyType: {}",
                userId, currentPromotionBalance, wallet.getPromotionBalance(), historyType);
    }
}
