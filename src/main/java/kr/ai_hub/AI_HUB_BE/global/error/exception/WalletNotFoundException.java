package kr.ai_hub.AI_HUB_BE.global.error.exception;

import kr.ai_hub.AI_HUB_BE.global.error.ErrorCode;

public class WalletNotFoundException extends BaseException {

    public WalletNotFoundException() {
        super(ErrorCode.WALLET_NOT_FOUND);
    }

    public WalletNotFoundException(String message) {
        super(ErrorCode.WALLET_NOT_FOUND, message);
    }
}
