package kr.ai_hub.AI_HUB_BE.global.error.exception;

import kr.ai_hub.AI_HUB_BE.global.error.ErrorCode;

/**
 * AI 서버 통신 예외
 * <p>
 * AI 서버와의 HTTP 통신 중 발생하는 모든 에러를 처리합니다.
 * - 네트워크 에러
 * - AI 서버 응답 에러
 * - 타임아웃
 * </p>
 */
public class AIServerException extends BaseException {

    public AIServerException() {
        super(ErrorCode.AI_SERVER_ERROR);
    }

    public AIServerException(String message) {
        super(ErrorCode.AI_SERVER_ERROR, message);
    }

    public AIServerException(String message, Throwable cause) {
        super(ErrorCode.AI_SERVER_ERROR, message, cause);
    }
}
