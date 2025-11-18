package kr.ai_hub.AI_HUB_BE.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 에러 코드 정의
 * <p>
 * HttpStatus는 GlobalExceptionHandler에서 결정됩니다.
 * ErrorCode는 code(에러코드)와 message(에러메시지)만 포함합니다.
 * </p>
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 인증 전 (Public API) - 보안을 위해 일반화된 메시지
    AUTHENTICATION_FAILED("AUTH_001", "인증에 실패했습니다"),
    VALIDATION_ERROR("VALID_001", "입력값 검증에 실패했습니다"),
    UNSUPPORTED_OAUTH2_PROVIDER("AUTH_002", "지원하지 않는 OAuth2 공급자입니다"),

    // 인증 후 (Authenticated API) - 구체적인 메시지
    USER_NOT_FOUND("USER_001", "사용자를 찾을 수 없습니다"),
    INSUFFICIENT_BALANCE("PAYMENT_001", "코인 잔액이 부족합니다"),
    ROOM_NOT_FOUND("ROOM_001", "채팅방을 찾을 수 없습니다"),
    MESSAGE_NOT_FOUND("MSG_001", "메시지를 찾을 수 없습니다"),
    MODEL_NOT_FOUND("MODEL_001", "AI 모델을 찾을 수 없습니다"),
    WALLET_NOT_FOUND("WALLET_001", "지갑을 찾을 수 없습니다"),
    PAYMENT_NOT_FOUND("PAYMENT_002", "결제 내역을 찾을 수 없습니다"),
    PAYMENT_FAILED("PAYMENT_003", "결제에 실패했습니다"),
    INVALID_TOKEN("AUTH_003", "유효하지 않은 토큰입니다"),
    FORBIDDEN("AUTH_004", "접근 권한이 없습니다"),
    TRANSACTION_NOT_FOUND("TRANS_001", "거래 내역을 찾을 수 없습니다"),
    SYSTEM_ILLEGAL_STATE("SYS_001", "시스템 상태가 유효하지 않습니다"),

    // 외부 서비스 오류
    AI_SERVER_ERROR("EXT_001", "AI 서버와의 통신에 실패했습니다"),

    // 공통 오류
    INTERNAL_SERVER_ERROR("SYS_002", "서버 내부 오류가 발생했습니다"),
    SERVICE_UNAVAILABLE("SYS_003", "서비스를 사용할 수 없습니다");

    private final String code;
    private final String message;
}
