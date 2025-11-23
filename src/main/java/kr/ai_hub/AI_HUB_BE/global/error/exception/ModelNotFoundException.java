package kr.ai_hub.AI_HUB_BE.global.error.exception;

import kr.ai_hub.AI_HUB_BE.global.error.ErrorCode;

public class ModelNotFoundException extends BaseException {

    public ModelNotFoundException() {
        super(ErrorCode.MODEL_NOT_FOUND);
    }

    public ModelNotFoundException(String message) {
        super(ErrorCode.MODEL_NOT_FOUND, message);
    }
}
