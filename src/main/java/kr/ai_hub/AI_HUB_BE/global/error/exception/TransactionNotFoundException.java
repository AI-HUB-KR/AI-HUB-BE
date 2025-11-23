package kr.ai_hub.AI_HUB_BE.global.error.exception;

import kr.ai_hub.AI_HUB_BE.global.error.ErrorCode;

public class TransactionNotFoundException extends BaseException {

    public TransactionNotFoundException() {
        super(ErrorCode.TRANSACTION_NOT_FOUND);
    }

    public TransactionNotFoundException(String message) {
        super(ErrorCode.TRANSACTION_NOT_FOUND, message);
    }
}
