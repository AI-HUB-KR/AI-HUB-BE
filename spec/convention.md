## 아키텍처 참고사항

- **패키지 네이밍**: Java 패키지명에는 하이픈이 아닌 언더스코어(`kr.ai_hub.AI_HUB_BE`)를 항상 사용해야 합니다
- **메인 애플리케이션**: 진입점은 표준 `@SpringBootApplication` 어노테이션이 적용된 `AiHubBeApplication.java`입니다
- **엔티티 설계**: JPA 엔티티 클래스 작성 시 위의 데이터베이스 구조를 정확히 따라야 합니다
  - UUID 필드는 `UUID` 타입 사용
  - DECIMAL 필드는 `BigDecimal` 타입 사용
  - JSONB 필드는 적절한 컨버터 또는 하이버네이트 타입 사용
- **Virtual Threads (Java 21+)**: 프로젝트는 Virtual Threads를 활성화하여 동시성을 처리합니다
  - `application.yaml`에 `spring.threads.virtual.enabled: true` 설정
  - `@Async` 어노테이션 사용 금지 (Virtual Threads가 자동으로 처리)
  - 동기 방식 코드 작성 (Virtual Threads가 I/O 블로킹 자동 처리)
  - 블로킹 I/O 작업을 평범한 동기 코드로 작성 (예: `WebClient.toStream()`)
- **외부 MSA 통신**: AI 서버와의 통신은 WebClient를 사용합니다
  - WebClient는 HTTP 클라이언트로만 사용 (Reactive Stack 사용 안 함)
  - `global/config/WebClientConfig.java`에서 설정 관리
  - SSE(Server-Sent Events) 스트리밍 지원
- Response 및 Error 클래스들은 from 및 builder 패턴을 활용하여 일관된 생성 방식을 유지합니다
- 추가적인 패키지나 디렉토리가 필요할 경우 `convention.md`에 명시된 구조를 참고하여 일관성 있게 확장합니다

## API 응답 구조 표준

모든 API는 일관된 구조의 응답을 반환합니다.

### 성공 응답 구조
```json
{
  "success": true,
  "data": {
    // 실제 응답 데이터
  },
  "message": "요청이 성공적으로 처리되었습니다",
  "timestamp": "2025-01-01T00:00:00Z"
}
```

### 실패 응답 구조
```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "오류 메시지",
    "details": "상세 오류 정보 (선택적)"
  },
  "timestamp": "2025-01-01T00:00:00Z"
}
```

### 업데이트 대상 작업
- 엔티티 추가 또는 수정
- API 엔드포인트 구현
- 프로젝트 구조 변경
- 의존성 추가 또는 변경
- 설정 파일 수정
- 주요 기능 구현

### 업데이트 시 반영할 내용
- **프로젝트 구조**: 새로 추가된 패키지 및 디렉토리 구조
- **데이터베이스 구조**: 새로운 엔티티, 필드 변경, 관계 수정
- **API 엔드포인트**: 구현된 REST API 목록 및 사용법
- **의존성**: build.gradle에 추가된 새로운 라이브러리
- **설정**: application.yaml 또는 기타 설정 파일의 중요 변경사항
- **명령어**: 새로 추가된 Gradle 태스크나 스크립트






# Error & Exception Convention

## 1. 에러 코드 정의 (ErrorCode Enum)

모든 에러 코드는 `global/error/ErrorCode.java` Enum에 중앙 집중식으로 정의합니다.

```java
@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "인증에 실패했습니다"),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "입력값 검증에 실패했습니다"),

    // 외부 서비스 오류
    AI_SERVER_ERROR(HttpStatus.BAD_GATEWAY, "AI 서버와의 통신에 실패했습니다"),
    // ...

    private final HttpStatus status;
    private final String message;
}
```

### 에러 코드 네이밍 규칙
- **대문자 스네이크 케이스** 사용 (예: `ROOM_NOT_FOUND`)
- **명확하고 구체적인 이름** 사용
- **도메인별 접두사** 고려 (예: `PAYMENT_FAILED`, `TOKEN_INVALID`)

### 보안 원칙
- **인증 전 API**: 일반화된 에러 코드 사용 (예: `AUTHENTICATION_FAILED`, `VALIDATION_ERROR`)
  - 시스템 내부 정보 노출 방지 (사용자 열거 공격 방지)
- **인증 후 API**: 구체적인 에러 코드 사용 가능 (예: `ROOM_NOT_FOUND`, `INSUFFICIENT_BALANCE`)

## 2. 예외 클래스 계층 구조

### BaseException (추상 클래스)
모든 커스텀 예외는 `BaseException`을 상속합니다.

```java
public abstract class BaseException extends RuntimeException {
    private final ErrorCode errorCode;

    protected BaseException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
```

### 도메인별 예외 클래스
각 도메인별로 구체적인 예외 클래스를 생성합니다.

**위치**: `global/error/exception/`

**네이밍 규칙**: `{도메인명}Exception` (예: `RoomNotFoundException`, `InsufficientBalanceException`)

**구현 예시**:
```java
public class RoomNotFoundException extends BaseException {
    public RoomNotFoundException() {
        super(ErrorCode.ROOM_NOT_FOUND);
    }

    public RoomNotFoundException(String message) {
        super(ErrorCode.ROOM_NOT_FOUND, message);
    }
}

// 외부 서비스 통신 예외
public class AIServerException extends BaseException {
    public AIServerException() {
        super(ErrorCode.AI_SERVER_ERROR);
    }

    public AIServerException(String message) {
        super(ErrorCode.AI_SERVER_ERROR, message);
    }

    public AIServerException(String message, Throwable cause) {
        super(ErrorCode.AI_SERVER_ERROR, message, cause);
    }
}
```

### 예외 사용 규칙
1. **비즈니스 로직 예외는 항상 커스텀 예외를 사용**
   ```java
   // Good
   throw new RoomNotFoundException();

   // Bad
   throw new RuntimeException("채팅방을 찾을 수 없습니다");
   ```

2. **커스텀 메시지가 필요한 경우 생성자 오버로딩 활용**
   ```java
   throw new ValidationException("제목은 30자 이내여야 합니다");
   ```

3. **표준 Java 예외는 적절한 경우에만 사용**
   - `IllegalArgumentException`: 메서드 인자 검증 실패
   - `IllegalStateException`: 객체 상태가 메서드 호출에 부적합

## 3. 응답 구조

### ApiResponse (통합 응답 객체)

**위치**: `global/common/response/ApiResponse.java`

모든 API는 `ApiResponse<T>` 객체로 응답합니다.

**구조**:
```java
public record ApiResponse<T>(
    boolean success,
    T detail,
    Instant timestamp
) { }
```

**필드 설명**:
- `success`: 요청 성공 여부
- `detail`: 성공 시 실제 데이터, 실패 시 ErrorResponse
- `timestamp`: 응답 생성 시각

### 성공 응답 생성
```java
// 데이터 없이 성공
return ResponseEntity.ok(ApiResponse.ok());

// 데이터와 함께 성공
return ResponseEntity.ok(ApiResponse.ok(data));

// 201 Created
return ResponseEntity
    .status(HttpStatus.CREATED)
    .body(ApiResponse.ok(data));
```

### ErrorResponse (에러 응답 객체)

**위치**: `global/response/ErrorResponse.java`

```java
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
    String code,
    String message,
    String details  // 선택적
) { }
```

### 에러 응답 생성
```java
// ErrorCode만 사용 (가장 일반적)
ApiResponse.error(ErrorCode.ROOM_NOT_FOUND);

// ErrorCode + 상세 정보
ApiResponse.error(ErrorCode.VALIDATION_ERROR, "title: 제목은 필수입니다");

// 단순 메시지로 에러 생성
ApiResponse.error("처리 중 오류가 발생했습니다");

// ErrorResponse 객체로 에러 생성
ErrorResponse errorResponse = ErrorResponse.of(ErrorCode.INSUFFICIENT_BALANCE, "필요 금액: 10 코인");
ApiResponse.error(errorResponse);
```

## 4. GlobalExceptionHandler

**위치**: `global/error/GlobalExceptionHandler.java`

`@RestControllerAdvice`를 사용하여 전역 예외를 처리합니다.

### 처리되는 예외 타입
1. **IllegalSystemStateException**: 시스템 상태 불일치 (토큰 해싱 실패 등)
2. **BaseException**: 모든 커스텀 비즈니스 예외
3. **MethodArgumentNotValidException**: `@Valid` 검증 실패
4. **IllegalArgumentException**: 잘못된 인자
5. **Exception**: 예상치 못한 모든 예외

### 예외 처리 흐름
```
예외 발생 → GlobalExceptionHandler 캐치
→ ErrorCode 추출 → ErrorResponse 생성
→ ApiResponse<ErrorResponse>로 래핑 → HTTP 응답
```

### 모든 예외 핸들러의 반환 타입
```java
public ResponseEntity<ApiResponse<ErrorResponse>> handle...Exception(...) {
    ApiResponse<ErrorResponse> response = ApiResponse.error(...);
    return ResponseEntity.status(...).body(response);
}
```

## 5. 사용 예시

### 컨트롤러에서 예외 발생
```java
@RestController
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    @GetMapping("/api/chat-rooms/{roomId}")
    public ResponseEntity<ApiResponse<ChatRoomResponse>> getChatRoom(@PathVariable UUID roomId) {
        ChatRoomResponse response = chatRoomService.getChatRoom(roomId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
```

### 서비스에서 예외 발생
```java
@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;

    public ChatRoomResponse getChatRoom(UUID roomId) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> new RoomNotFoundException());

        return ChatRoomResponse.from(chatRoom);
    }

    public void sendMessage(UUID roomId, BigDecimal requiredCoin) {
        UserWallet wallet = getWallet();

        if (wallet.getBalance().compareTo(requiredCoin) < 0) {
            throw new InsufficientBalanceException(
                String.format("필요 코인: %.4f, 현재 잔액: %.4f", requiredCoin, wallet.getBalance())
            );
        }

        // 메시지 전송 로직
    }
}
```

### Validation 예외
```java
@PostMapping("/api/chat-rooms")
public ResponseEntity<ApiResponse<ChatRoomResponse>> createChatRoom(
    @Valid @RequestBody CreateChatRoomRequest request) {

    ChatRoomResponse response = chatRoomService.createChatRoom(request);
    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(ApiResponse.success(response, "채팅방이 생성되었습니다"));
}

// DTO
public record CreateChatRoomRequest(
    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 30, message = "제목은 30자 이내여야 합니다")
    String title,

    @NotNull(message = "모델 ID는 필수입니다")
    Integer modelId
) { }
```

## 6. 로깅 규칙 (SLF4J + Logback)

### 6.1 로거 선언

프로젝트는 **SLF4J + Logback**을 사용하며, Lombok의 `@Slf4j` 어노테이션을 사용하여 로거를 선언합니다.

```java
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class MyService {
    public void doSomething() {
        log.info("Doing something");
    }
}
```

**참고**: Spring Boot는 기본적으로 `spring-boot-starter-logging`을 포함하므로 별도의 SLF4J 의존성 추가가 불필요합니다.

### 6.2 로그 레벨

| 레벨 | 용도 | 예시 |
|------|------|------|
| **ERROR** | 시스템 오류, 예상치 못한 예외 | `Exception` 발생, 시스템 장애 |
| **WARN** | 비즈니스 예외, Validation 실패 | `BaseException`, `MethodArgumentNotValidException` |
| **INFO** | 정상 처리 흐름, 중요 이벤트 | 사용자 로그인, 결제 완료, API 요청/응답 |
| **DEBUG** | 상세 디버깅 정보 | 메서드 파라미터, 중간 계산 결과, 토큰 생성/검증 |
| **TRACE** | 매우 상세한 정보 (거의 사용 안 함) | SQL 파라미터 바인딩 |

### 6.3 로그 메시지 작성 규칙

#### 기본 원칙
- **명확하고 간결한 메시지** 작성
- **파라미터화된 로깅** 사용 (성능 향상)
- **민감 정보 로깅 금지** (비밀번호, 토큰 원본, 개인정보)

#### 좋은 예시
```java
// Good - 파라미터화된 로깅
log.info("사용자 {} 로그인 성공", userId);
log.debug("사용자 {} 액세스 토큰 생성 중", user.getUserId());
log.warn("사용자 {} 리프레시 토큰 검증 실패: {}", userId, e.getMessage());
log.error("JWT 토큰 파싱 실패: {}", e.getMessage());

// Good - 예외와 함께 로깅
try {
    // ...
} catch (Exception e) {
    log.error("예상치 못한 예외 발생: {}", e.getMessage(), e);
    throw e;
}
```

#### 나쁜 예시
```java
// Bad - 문자열 연결 사용 (성능 저하)
log.info("사용자 로그인: " + userId);

// Bad - 민감 정보 로깅
log.debug("JWT 토큰: {}", jwtToken); // 토큰 원본 노출
log.debug("사용자 비밀번호: {}", password); // 비밀번호 노출

// Bad - 너무 장황한 메시지
log.info("사용자 ID " + userId + "가 " + LocalDateTime.now() + " 시각에 성공적으로 로그인했습니다");
```

### 6.4 레이어별 로깅 가이드

#### Controller 레이어
```java
@Slf4j
@RestController
@RequiredArgsConstructor
public class TokenController {

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken() {
        log.info("토큰 갱신 API 호출");

        try {
            // 비즈니스 로직
            log.info("토큰 갱신 성공");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.warn("토큰 갱신 실패: {}", e.getMessage());
            throw e;
        }
    }
}
```

#### Service 레이어
```java
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    public RefreshedTokens refreshAccessToken(String rawRefreshToken) {
        log.info("토큰 갱신 요청");

        try {
            Claims claims = jwtTokenProvider.parseClaims(rawRefreshToken);
            log.debug("사용자 {} 토큰 갱신 중", claims.getSubject());

            // 비즈니스 로직

            log.info("사용자 {} 토큰 갱신 완료", userId);
            return tokens;
        } catch (JwtException e) {
            log.warn("유효하지 않은 리프레시 토큰: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }
    }
}
```

#### Domain/Repository 레이어
- 일반적으로 로그 불필요 (JPA 쿼리는 `application.yaml`에서 `show-sql` 설정으로 처리)
- 복잡한 쿼리나 성능 이슈가 있는 경우에만 DEBUG 레벨 로그 추가

### 6.5 GlobalExceptionHandler 로깅

```java
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiResponse<Void>> handleBaseException(BaseException e) {
        log.warn("비즈니스 예외 발생: {}", e.getMessage(), e);
        // ...
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("예상치 못한 예외 발생: {}", e.getMessage(), e);
        // ...
    }
}
```

### 6.6 민감 정보 로깅 주의사항

다음 정보는 **절대 로그에 남기지 않습니다**:

- ❌ 비밀번호 (평문, 해시 모두)
- ❌ JWT 토큰 원본 (Access Token, Refresh Token)
- ❌ OAuth2 Access Token
- ❌ API Secret Key
- ❌ 개인정보 (주민등록번호, 신용카드 번호 등)

**대신 사용할 수 있는 정보**:
- ✅ 사용자 ID (userId)
- ✅ 이메일 (필요 시 마스킹: `us**@example.com`)
- ✅ 토큰 해시 (해시된 값의 일부만)
- ✅ 예외 메시지

### 6.7 로그 설정 (application.yaml)

```yaml
logging:
  level:
    root: INFO
    kr.ai_hub.AI_HUB_BE: DEBUG
    org.springframework.web: DEBUG
    org.springframework.security: INFO
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: logs/ai-hub.log
    max-size: 10MB
    max-history: 30
```

### 6.8 체크리스트

새로운 클래스에 로깅을 추가할 때:

- [ ] `@Slf4j` 어노테이션 추가
- [ ] INFO 레벨: 중요 비즈니스 이벤트 로깅
- [ ] DEBUG 레벨: 상세 디버깅 정보 로깅
- [ ] WARN 레벨: 비즈니스 예외 로깅
- [ ] ERROR 레벨: 시스템 오류 로깅
- [ ] 파라미터화된 로깅 사용 (`{}` 플레이스홀더)
- [ ] 민감 정보 로깅 제외

## 7. 체크리스트

새로운 에러 처리가 필요할 때 다음을 확인하세요:

- [ ] `ErrorCode` Enum에 에러 코드 추가
- [ ] 필요시 커스텀 예외 클래스 생성 (`BaseException` 상속)
- [ ] 서비스 레이어에서 적절한 예외 발생
- [ ] 컨트롤러는 예외를 처리하지 않고 `GlobalExceptionHandler`에 위임
- [ ] 인증 전/후 보안 원칙 준수
- [ ] 응답은 항상 `ApiResponse`로 래핑
### 시스템 상태 예외 대응

토큰 해싱 등 내부 인프라 의존 로직에서 `NoSuchAlgorithmException` 등으로 시스템 상태가 불일치할 경우,
`IllegalSystemStateException`을 발생시켜 `ErrorCode.SYSTEM_ILLEGAL_STATE` 응답을 반환합니다.

```java
import kr.ai_hub.AI_HUB_BE.global.error.exception.IllegalSystemStateException;

try {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    // ...
} catch (NoSuchAlgorithmException e) {
    throw new IllegalSystemStateException("SHA-256 MessageDigest not available", e);
}
```
