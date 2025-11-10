package kr.ai_hub.AI_HUB_BE.global.error.exception;

import kr.ai_hub.AI_HUB_BE.global.error.ErrorCode;

public class ValidationException extends BaseException {

    public ValidationException() {
        super(ErrorCode.VALIDATION_ERROR);
    }

    public ValidationException(String message) {
        super(ErrorCode.VALIDATION_ERROR, message);
    }
}
