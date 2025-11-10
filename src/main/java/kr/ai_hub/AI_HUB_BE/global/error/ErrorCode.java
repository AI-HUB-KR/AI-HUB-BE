package kr.ai_hub.AI_HUB_BE.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 인증 전 (Public API) - 보안을 위해 일반화된 메시지
    AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "인증에 실패했습니다"),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "입력값 검증에 실패했습니다"),
    UNSUPPORTED_OAUTH2_PROVIDER(HttpStatus.BAD_REQUEST, "지원하지 않는 OAuth2 공급자입니다"),

    // 인증 후 (Authenticated API) - 구체적인 메시지
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"),
    INSUFFICIENT_BALANCE(HttpStatus.BAD_REQUEST, "코인 잔액이 부족합니다"),
    ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "채팅방을 찾을 수 없습니다"),
    MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "메시지를 찾을 수 없습니다"),
    MODEL_NOT_FOUND(HttpStatus.NOT_FOUND, "AI 모델을 찾을 수 없습니다"),
    PAYMENT_FAILED(HttpStatus.BAD_REQUEST, "결제에 실패했습니다"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다"),
    TRANSACTION_NOT_FOUND(HttpStatus.NOT_FOUND, "거래 내역을 찾을 수 없습니다"),
    SYSTEM_ILLEGAL_STATE(HttpStatus.CONFLICT, "시스템 상태가 유효하지 않습니다"),

    // 공통 오류
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다"),
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "서비스를 사용할 수 없습니다");

    private final HttpStatus status;
    private final String message;
}
