package kr.ai_hub.AI_HUB_BE.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import kr.ai_hub.AI_HUB_BE.global.error.ErrorCode;
import lombok.Builder;

import java.time.Instant;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        T data,
        String message,
        ErrorResponse error,
        Instant timestamp
) {

    private static final String DEFAULT_SUCCESS_MESSAGE = "요청이 성공적으로 처리되었습니다";

    // 성공 응답 - 데이터만
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .message(DEFAULT_SUCCESS_MESSAGE)
                .timestamp(Instant.now())
                .build();
    }

    // 성공 응답 - 데이터와 메시지
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .message(message)
                .timestamp(Instant.now())
                .build();
    }

    // 실패 응답 - ErrorCode
    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(ErrorResponse.of(errorCode))
                .timestamp(Instant.now())
                .build();
    }

    // 실패 응답 - ErrorCode와 상세 내용
    public static <T> ApiResponse<T> error(ErrorCode errorCode, String details) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(ErrorResponse.of(errorCode, details))
                .timestamp(Instant.now())
                .build();
    }

    // 실패 응답 - ErrorResponse 직접 전달
    public static <T> ApiResponse<T> error(ErrorResponse errorResponse) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(errorResponse)
                .timestamp(Instant.now())
                .build();
    }
}
