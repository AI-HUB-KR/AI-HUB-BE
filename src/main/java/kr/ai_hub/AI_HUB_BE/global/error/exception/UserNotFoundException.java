package kr.ai_hub.AI_HUB_BE.global.error.exception;

import kr.ai_hub.AI_HUB_BE.global.error.ErrorCode;

public class UserNotFoundException extends BaseException {

    public UserNotFoundException() {
        super(ErrorCode.USER_NOT_FOUND);
    }

    public UserNotFoundException(String message) {
        super(ErrorCode.USER_NOT_FOUND, message);
    }
}
