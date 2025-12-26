package kr.ai_hub.AI_HUB_BE.admin.dto;

import kr.ai_hub.AI_HUB_BE.user.domain.User;
import kr.ai_hub.AI_HUB_BE.user.domain.UserRole;
import kr.ai_hub.AI_HUB_BE.wallet.domain.UserWallet;

import java.math.BigDecimal;

/**
 * 사용자 정보 전체 리스트 응답 DTO
 */
public record UserListResponse(
        // User 정보
        Integer userId,
        String username,
        UserRole role,
        String email,

        // Wallet 정보
        Integer walletId,
        BigDecimal balance,
        BigDecimal paidBalance,
        BigDecimal promotionBalance,
        BigDecimal totalPurchased,
        BigDecimal totalUsed
) {
    /**
     * User와 UserWallet 엔티티로부터 UserListResponse를 생성합니다.
     *
     * @param user   사용자 엔티티
     * @param wallet 지갑 엔티티
     * @return UserListResponse
     */
    public static UserListResponse from(User user, UserWallet wallet) {
        return new UserListResponse(
                user.getUserId(),
                user.getUsername(),
                user.getRole(),
                user.getEmail(),
                wallet != null ? wallet.getWalletId() : null,
                wallet != null ? wallet.getBalance() : BigDecimal.ZERO,
                wallet != null ? wallet.getPaidBalance() : BigDecimal.ZERO,
                wallet != null ? wallet.getPromotionBalance() : BigDecimal.ZERO,
                wallet != null ? wallet.getTotalPurchased() : BigDecimal.ZERO,
                wallet != null ? wallet.getTotalUsed() : BigDecimal.ZERO
        );
    }
}
