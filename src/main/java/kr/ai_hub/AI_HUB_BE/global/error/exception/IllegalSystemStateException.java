package kr.ai_hub.AI_HUB_BE.global.error.exception;

import kr.ai_hub.AI_HUB_BE.global.error.ErrorCode;

public class IllegalSystemStateException extends BaseException {

    public IllegalSystemStateException() {
        super(ErrorCode.SYSTEM_ILLEGAL_STATE);
    }

    public IllegalSystemStateException(String message) {
        super(ErrorCode.SYSTEM_ILLEGAL_STATE, message);
    }

    public IllegalSystemStateException(String message, Throwable cause) {
        super(ErrorCode.SYSTEM_ILLEGAL_STATE, message, cause);
    }
}
