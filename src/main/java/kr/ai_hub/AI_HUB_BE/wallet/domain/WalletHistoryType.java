package kr.ai_hub.AI_HUB_BE.wallet.domain;

/**
 * 지갑 내역 타입
 */
public enum WalletHistoryType {
    /**
     * 유상 코인 지급 (결제)
     */
    PAID,

    /**
     * 프로모션 코인 지급 (관리자)
     */
    PROMOTION,

    /**
     * 프로모션 코인 회수 (관리자)
     */
    PROMOTION_RETRIEVE,

    /**
     * 유상 코인 환불 (관리자)
     */
    PAID_RETRIEVE
}
