package kr.ai_hub.AI_HUB_BE.global.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import kr.ai_hub.AI_HUB_BE.global.error.ErrorCode;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String code,
        String message,
        String details
) {

    public static ErrorResponse of(ErrorCode errorCode) {
        return ErrorResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();
    }

    public static ErrorResponse of(ErrorCode errorCode, String details) {
        return ErrorResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .details(details)
                .build();
    }

    public static ErrorResponse of(String code, String message) {
        return ErrorResponse.builder()
                .code(code)
                .message(message)
                .build();
    }

    public static ErrorResponse of(String code, String message, String details) {
        return ErrorResponse.builder()
                .code(code)
                .message(message)
                .details(details)
                .build();
    }
}
