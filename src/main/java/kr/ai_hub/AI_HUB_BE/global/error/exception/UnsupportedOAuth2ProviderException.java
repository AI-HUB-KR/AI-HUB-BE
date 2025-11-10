package kr.ai_hub.AI_HUB_BE.global.error.exception;

import kr.ai_hub.AI_HUB_BE.global.error.ErrorCode;

public class UnsupportedOAuth2ProviderException extends BaseException {

    public UnsupportedOAuth2ProviderException() {
        super(ErrorCode.UNSUPPORTED_OAUTH2_PROVIDER);
    }

    public UnsupportedOAuth2ProviderException(String message) {
        super(ErrorCode.UNSUPPORTED_OAUTH2_PROVIDER, message);
    }
}
