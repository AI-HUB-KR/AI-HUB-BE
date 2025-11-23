package kr.ai_hub.AI_HUB_BE.global.error;

import kr.ai_hub.AI_HUB_BE.global.error.exception.AIServerException;
import kr.ai_hub.AI_HUB_BE.global.error.exception.BaseException;
import kr.ai_hub.AI_HUB_BE.global.error.exception.IllegalSystemStateException;
import kr.ai_hub.AI_HUB_BE.global.common.response.ApiResponse;
import kr.ai_hub.AI_HUB_BE.global.common.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * ErrorCode를 HttpStatus로 매핑합니다.
     *
     * @param errorCode 에러 코드
     * @return HTTP 상태 코드
     */
    private HttpStatus resolveHttpStatus(ErrorCode errorCode) {
        return switch (errorCode) {
            // 401 UNAUTHORIZED
            case AUTHENTICATION_FAILED, INVALID_TOKEN -> HttpStatus.UNAUTHORIZED;

            // 403 FORBIDDEN
            case FORBIDDEN -> HttpStatus.FORBIDDEN;

            // 404 NOT_FOUND
            case USER_NOT_FOUND, ROOM_NOT_FOUND, MESSAGE_NOT_FOUND,
                 MODEL_NOT_FOUND, WALLET_NOT_FOUND, PAYMENT_NOT_FOUND,
                 TRANSACTION_NOT_FOUND -> HttpStatus.NOT_FOUND;

            // 409 CONFLICT
            case SYSTEM_ILLEGAL_STATE -> HttpStatus.CONFLICT;

            // 502 BAD_GATEWAY
            case AI_SERVER_ERROR -> HttpStatus.BAD_GATEWAY;

            // 503 SERVICE_UNAVAILABLE
            case SERVICE_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;

            // 500 INTERNAL_SERVER_ERROR
            case INTERNAL_SERVER_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;

            // 400 BAD_REQUEST (default for validation, business logic errors)
            default -> HttpStatus.BAD_REQUEST;
        };
    }

    // BaseException 및 모든 서브클래스 처리
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleBaseException(BaseException e) {
        // 중요한 예외는 ERROR 레벨로 로깅
        if (e instanceof AIServerException || e instanceof IllegalSystemStateException) {
            log.error("중요 예외 발생: {}", e.getMessage(), e);
        } else {
            log.warn("비즈니스 예외 발생: {}", e.getMessage(), e);
        }

        ErrorCode errorCode = e.getErrorCode();
        ApiResponse<ErrorResponse> response = ApiResponse.error(errorCode, e.getMessage());
        HttpStatus status = resolveHttpStatus(errorCode);

        return ResponseEntity
                .status(status)
                .body(response);
    }

    // Validation 예외 처리 (@Valid 검증 실패)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleValidationException(MethodArgumentNotValidException e) {
        log.warn("입력값 검증 실패: {}", e.getMessage());

        String details = e.getBindingResult()
                .getAllErrors()
                .stream()
                .map(error -> {
                    if (error instanceof FieldError fieldError) {
                        return fieldError.getField() + ": " + error.getDefaultMessage();
                    }
                    return error.getDefaultMessage();
                })
                .collect(Collectors.joining(", "));

        ApiResponse<ErrorResponse> response = ApiResponse.error(ErrorCode.VALIDATION_ERROR, details);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    // 일반 예외 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleException(Exception e) {
        log.error("예상치 못한 예외 발생: {}", e.getMessage(), e);

        ApiResponse<ErrorResponse> response = ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }

    // IllegalArgumentException 처리
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("잘못된 인자 전달: {}", e.getMessage());

        ErrorResponse errorResponse = ErrorResponse.of(
                ErrorCode.VALIDATION_ERROR.getCode(),
                ErrorCode.VALIDATION_ERROR.getMessage(),
                e.getMessage()
        );

        ApiResponse<ErrorResponse> response = ApiResponse.error(errorResponse);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    // ResponseStatusException 처리 (안전망 - 모든 ResponseStatusException은 커스텀 예외로 대체되어야 함)
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleResponseStatusException(ResponseStatusException e) {
        log.warn("ResponseStatusException 발생 (커스텀 예외로 전환 필요): {}", e.getReason());

        HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        ErrorResponse errorResponse = ErrorResponse.of(
                "RESPONSE_STATUS_EXCEPTION",
                e.getReason() != null ? e.getReason() : "요청 처리 중 오류가 발생했습니다",
                null
        );

        ApiResponse<ErrorResponse> response = ApiResponse.error(errorResponse);

        return ResponseEntity
                .status(status)
                .body(response);
    }
}
