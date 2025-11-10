package kr.ai_hub.AI_HUB_BE.global.common.response;

import kr.ai_hub.AI_HUB_BE.global.error.ErrorCode;

import java.time.Instant;

/**
 * <h4>통합 API 응답 객체</h4>
 *
 * 성공 시: detail에 실제 데이터
 * 실패 시: detail에 ErrorResponse
 */
public record ApiResponse<T>(boolean success, T detail, Instant timestamp) {

    // ========== 성공 응답 ==========

    // 데이터 없이 성공 응답 생성
    public static <T> ApiResponse<T> ok() {
        return new ApiResponse<>(true, null, Instant.now());
    }


    // 데이터를 포함한 성공 응답 생성
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, Instant.now());
    }

    // ========== 에러 응답 ==========

    // ErrorCode로 에러 응답 생성
    public static ApiResponse<ErrorResponse> error(ErrorCode errorCode) {
        return new ApiResponse<>(false, ErrorResponse.of(errorCode), Instant.now());
    }

    // ErrorCode와 상세 메시지로 에러 응답 생성
    public static ApiResponse<ErrorResponse> error(ErrorCode errorCode, String details) {
        return new ApiResponse<>(false, ErrorResponse.of(errorCode, details), Instant.now());
    }

    // 코드와 메시지로 에러 응답 생성
    public static ApiResponse<ErrorResponse> error(String message) {
        return new ApiResponse<>(false, ErrorResponse.of("UNKNOWN_ERROR", message), Instant.now());
    }

    // ErrorResponse로 에러 응답 생성
    public static ApiResponse<ErrorResponse> error(ErrorResponse errorResponse) {
        return new ApiResponse<>(false, errorResponse, Instant.now());
    }
}
