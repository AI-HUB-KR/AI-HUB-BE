package kr.ai_hub.AI_HUB_BE.global.error.exception;

import kr.ai_hub.AI_HUB_BE.global.error.ErrorCode;

public class AuthenticationFailedException extends BaseException {

    public AuthenticationFailedException() {
        super(ErrorCode.AUTHENTICATION_FAILED);
    }

    public AuthenticationFailedException(String message) {
        super(ErrorCode.AUTHENTICATION_FAILED, message);
    }
}
