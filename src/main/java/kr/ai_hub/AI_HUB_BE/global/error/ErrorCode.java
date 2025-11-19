package kr.ai_hub.AI_HUB_BE.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 에러 코드 정의
 * <p>
 * ErrorCode는 enum 이름을 code로 사용합니다.
 * HttpStatus는 GlobalExceptionHandler에서 결정됩니다.
 * </p>
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 인증 전 (Public API) - 보안을 위해 일반화된 메시지
    AUTHENTICATION_FAILED("인증에 실패했습니다"),
    VALIDATION_ERROR("입력값 검증에 실패했습니다"),
    UNSUPPORTED_OAUTH2_PROVIDER("지원하지 않는 OAuth2 공급자입니다"),

    // 인증 후 (Authenticated API) - 구체적인 메시지
    USER_NOT_FOUND("사용자를 찾을 수 없습니다"),
    INSUFFICIENT_BALANCE("코인 잔액이 부족합니다"),
    ROOM_NOT_FOUND("채팅방을 찾을 수 없습니다"),
    MESSAGE_NOT_FOUND("메시지를 찾을 수 없습니다"),
    MODEL_NOT_FOUND("AI 모델을 찾을 수 없습니다"),
    WALLET_NOT_FOUND("지갑을 찾을 수 없습니다"),
    PAYMENT_NOT_FOUND("결제 내역을 찾을 수 없습니다"),
    PAYMENT_FAILED("결제에 실패했습니다"),
    INVALID_TOKEN("유효하지 않은 토큰입니다"),
    FORBIDDEN("접근 권한이 없습니다"),
    TRANSACTION_NOT_FOUND("거래 내역을 찾을 수 없습니다"),
    SYSTEM_ILLEGAL_STATE("시스템 상태가 유효하지 않습니다"),

    // 외부 서비스 오류
    AI_SERVER_ERROR("AI 서버와의 통신에 실패했습니다"),

    // 공통 오류
    INTERNAL_SERVER_ERROR("서버 내부 오류가 발생했습니다"),
    SERVICE_UNAVAILABLE("서비스를 사용할 수 없습니다");

    private final String message;

    /**
     * ErrorCode의 코드를 반환합니다.
     * enum 이름이 code로 사용됩니다.
     */
    public String getCode() {
        return this.name();
    }
}
