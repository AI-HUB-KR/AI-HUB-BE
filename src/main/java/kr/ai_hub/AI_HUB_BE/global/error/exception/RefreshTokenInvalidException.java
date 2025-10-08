package kr.ai_hub.AI_HUB_BE.global.error.exception;

import kr.ai_hub.AI_HUB_BE.global.error.ErrorCode;

public class RefreshTokenInvalidException extends BaseException {

    public RefreshTokenInvalidException() {
        super(ErrorCode.INVALID_TOKEN);
    }

    public RefreshTokenInvalidException(String message) {
        super(ErrorCode.INVALID_TOKEN, message);
    }
}
