package kr.ai_hub.AI_HUB_BE.global.error.exception;

import kr.ai_hub.AI_HUB_BE.global.error.ErrorCode;

public class PaymentFailedException extends BaseException {

    public PaymentFailedException() {
        super(ErrorCode.PAYMENT_FAILED);
    }

    public PaymentFailedException(String message) {
        super(ErrorCode.PAYMENT_FAILED, message);
    }
}
