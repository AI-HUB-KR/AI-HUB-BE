package kr.ai_hub.AI_HUB_BE.global.error.exception;

import kr.ai_hub.AI_HUB_BE.global.error.ErrorCode;

public class MessageNotFoundException extends BaseException {

    public MessageNotFoundException() {
        super(ErrorCode.MESSAGE_NOT_FOUND);
    }

    public MessageNotFoundException(String message) {
        super(ErrorCode.MESSAGE_NOT_FOUND, message);
    }
}
