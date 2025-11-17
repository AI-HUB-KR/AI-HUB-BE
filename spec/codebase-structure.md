# AI-HUB-BE 코드베이스 구조 가이드

> 이 문서는 코드베이스 탐색 시 빠르게 필요한 파일을 찾을 수 있도록 작성된 구조 가이드입니다.
>
> 마지막 업데이트: 2025-11-17

---

## 📋 목차

1. [프로젝트 개요](#-프로젝트-개요)
2. [아키텍처 패턴](#-아키텍처-패턴)
3. [레이어별 구조](#-레이어별-구조)
4. [도메인별 파일 맵](#-도메인별-파일-맵)
5. [주요 컴포넌트 위치](#-주요-컴포넌트-위치)
6. [설정 파일](#-설정-파일)
7. [코딩 컨벤션](#-코딩-컨벤션)

---

## 🎯 프로젝트 개요

**프로젝트명**: AI-HUB-BE
**기술 스택**: Spring Boot 3.5.6, Java 25, JPA/Hibernate, PostgreSQL
**아키텍처**: Layered Architecture (계층형 아키텍처)
**패키지 전략**: Package by Feature (도메인별 패키지 구조)

### 통계
- **전체 Java 파일**: 102개
- **Entity**: 10개
- **Repository**: 9개
- **Service**: 14개
- **Controller**: 11개
- **DTO**: 23개

---

## 🏗 아키텍처 패턴

```
┌─────────────────────────────────────────┐
│         Controller Layer                │  ← HTTP 요청/응답 처리
│    (Presentation / API Endpoints)       │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│        Application Layer                │  ← 비즈니스 로직, 트랜잭션
│     (Service + DTO + Use Cases)         │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│          Domain Layer                   │  ← 엔티티, 리포지토리
│     (Entity + Repository + Enum)        │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│          Infrastructure                 │  ← 데이터베이스
│        (PostgreSQL / H2)                │
└─────────────────────────────────────────┘
```

### 레이어 간 의존성 규칙
- Controller → Service (O)
- Service → Repository (O)
- Repository → Entity (O)
- **역방향 의존성 금지** (하위 레이어가 상위 레이어 참조 X)

---

## 📁 레이어별 구조

### 1. Controller Layer
**위치**: `src/main/java/kr/ai_hub/AI_HUB_BE/controller/`

```
controller/
├── admin/
│   └── aimodel/AdminAIModelController.java      # 관리자 AI 모델 관리 API
├── aimodel/AIModelController.java               # AI 모델 조회 API
├── auth/
│   ├── AuthController.java                      # OAuth2 로그인 API
│   └── TokenController.java                     # 토큰 갱신 API
├── chat/
│   ├── ChatRoomController.java                  # 채팅방 CRUD API
│   └── ChatMessageController.java               # 메시지 조회 API
├── cointransaction/CoinTransactionController.java # 코인 거래 내역 API
├── dashboard/DashboardController.java           # 대시보드 통계 API
├── paymenthistory/PaymentHistoryController.java # 결제 내역 API
├── user/UserController.java                     # 사용자 정보 API
└── userwallet/UserWalletController.java         # 지갑 조회 API
```

**역할**: HTTP 요청 처리, 입력 검증(@Valid), 응답 변환(ApiResponse)

---

### 2. Application Layer
**위치**: `src/main/java/kr/ai_hub/AI_HUB_BE/application/`

```
application/
├── admin/aimodel/
│   ├── AdminAIModelService.java                 # 관리자 AI 모델 서비스
│   └── dto/
│       ├── CreateAIModelRequest.java
│       └── UpdateAIModelRequest.java
├── aimodel/
│   ├── AIModelService.java                      # AI 모델 조회 서비스
│   └── dto/AIModelResponse.java
├── auth/
│   ├── CustomOAuth2UserService.java             # OAuth2 사용자 서비스
│   ├── TokenHashService.java                    # 토큰 해싱 서비스
│   ├── accesstoken/AccessTokenService.java
│   ├── refreshtoken/RefreshTokenService.java
│   └── dto/
│       ├── RefreshedTokens.java
│       └── TokenRefreshResponse.java
├── chatroom/
│   ├── ChatRoomService.java                     # 채팅방 CRUD 서비스
│   └── dto/
│       ├── ChatRoomListItemResponse.java
│       ├── ChatRoomResponse.java
│       ├── CreateChatRoomRequest.java
│       └── UpdateChatRoomRequest.java
├── cointransaction/
│   ├── CoinTransactionService.java              # 코인 거래 내역 서비스
│   └── dto/CoinTransactionResponse.java
├── dashboard/
│   ├── DashboardService.java                    # 대시보드 통계 서비스
│   └── dto/
│       ├── DailyUsageDetail.java
│       ├── ModelPricingResponse.java
│       ├── ModelUsageDetail.java
│       ├── MonthlyUsageResponse.java
│       ├── MostUsedModel.java
│       └── UserStatsResponse.java
├── message/
│   ├── MessageService.java                      # 메시지 조회 서비스
│   └── dto/
│       ├── MessageListItemResponse.java
│       └── MessageResponse.java
├── paymenthistory/
│   ├── PaymentHistoryService.java               # 결제 내역 서비스
│   └── dto/PaymentResponse.java
├── user/
│   ├── UserService.java                         # 사용자 정보 서비스
│   └── dto/
│       ├── UpdateUserRequest.java
│       └── UserResponse.java
└── userwallet/
    ├── UserWalletService.java                   # 사용자 지갑 서비스
    └── dto/
        ├── BalanceResponse.java
        └── UserWalletResponse.java
```

**역할**: 비즈니스 로직, 트랜잭션 관리(@Transactional), DTO 변환

---

### 3. Domain Layer
**위치**: `src/main/java/kr/ai_hub/AI_HUB_BE/domain/`

```
domain/
├── accesstoken/
│   ├── entity/AccessToken.java
│   └── repository/AccessTokenRepository.java
├── aimodel/
│   ├── entity/AIModel.java                      # AI 모델 엔티티
│   └── repository/AIModelRepository.java
├── chatroom/
│   ├── entity/ChatRoom.java                     # 채팅방 엔티티 (UUID v7)
│   └── repository/ChatRoomRepository.java
├── cointransaction/
│   ├── entity/CoinTransaction.java              # 코인 거래 엔티티
│   └── repository/CoinTransactionRepository.java
├── message/
│   ├── entity/Message.java                      # 메시지 엔티티 (UUID v7)
│   └── repository/MessageRepository.java
├── paymenthistory/
│   ├── entity/PaymentHistory.java               # 결제 내역 엔티티
│   └── repository/PaymentHistoryRepository.java
├── refreshtoken/
│   ├── entity/RefreshToken.java
│   └── repository/RefreshTokenRepository.java
├── token/Token.java                             # 토큰 공통 인터페이스
├── user/
│   ├── entity/
│   │   ├── User.java                            # 사용자 엔티티 (Soft Delete)
│   │   └── UserRole.java                        # Enum: ROLE_USER, ROLE_ADMIN
│   └── repository/UserRepository.java
└── userwallet/
    ├── entity/UserWallet.java                   # 사용자 지갑 엔티티
    └── repository/UserWalletRepository.java
```

**역할**: 도메인 모델, 데이터베이스 매핑, 비즈니스 규칙 캡슐화

---

### 4. Global Layer
**위치**: `src/main/java/kr/ai_hub/AI_HUB_BE/global/`

```
global/
├── application/GlobalApplication.java           # 전역 애플리케이션 설정
├── auth/
│   ├── SecurityContextHelper.java               # ✨ 보안 컨텍스트 헬퍼 (공통)
│   ├── jwt/
│   │   ├── JwtAuthenticationEntryPoint.java     # JWT 인증 실패 핸들러
│   │   ├── JwtAuthenticationFilter.java         # JWT 인증 필터
│   │   └── JwtTokenProvider.java                # JWT 토큰 생성/검증
│   └── userinfo/
│       ├── CustomOauth2User.java                # OAuth2 사용자 정보
│       └── OAuth2SuccessHandler.java            # OAuth2 성공 핸들러
├── common/
│   └── response/ApiResponse.java                # 공통 API 응답 래퍼
├── config/
│   ├── JpaConfig.java                           # JPA 설정 (Auditing)
│   ├── OpenApiConfig.java                       # Swagger/OpenAPI 설정
│   └── SecurityConfig.java                      # Spring Security 설정
└── error/
    ├── ErrorCode.java                           # 에러 코드 Enum
    ├── GlobalExceptionHandler.java              # 전역 예외 핸들러
    └── exception/
        ├── BaseException.java                   # 기본 예외 클래스
        ├── ForbiddenException.java
        ├── InsufficientBalanceException.java
        ├── MessageNotFoundException.java
        ├── ModelNotFoundException.java
        ├── PaymentNotFoundException.java
        ├── RoomNotFoundException.java
        ├── TokenNotFoundException.java
        ├── UserNotFoundException.java
        ├── ValidationException.java
        └── WalletNotFoundException.java
```

**역할**: 전역 설정, 공통 컴포넌트, 횡단 관심사(인증, 예외처리)

---

## 🗂 도메인별 파일 맵

### User (사용자)
```
Controller:  controller/user/UserController.java
Service:     application/user/UserService.java
Entity:      domain/user/entity/User.java
Repository:  domain/user/repository/UserRepository.java
DTOs:
  - application/user/dto/UpdateUserRequest.java
  - application/user/dto/UserResponse.java

API Endpoints:
  - GET    /api/v1/users/me           # 내 정보 조회
  - PUT    /api/v1/users/me           # 내 정보 수정
  - DELETE /api/v1/users/me           # 회원 탈퇴
```

### AIModel (AI 모델)
```
Controller:  controller/aimodel/AIModelController.java
Service:     application/aimodel/AIModelService.java
Entity:      domain/aimodel/entity/AIModel.java
Repository:  domain/aimodel/repository/AIModelRepository.java
DTOs:
  - application/aimodel/dto/AIModelResponse.java

API Endpoints:
  - GET /api/v1/models              # 활성 모델 목록
  - GET /api/v1/models/{modelId}   # 모델 상세
```

### Admin AIModel (관리자 AI 모델 관리)
```
Controller:  controller/admin/aimodel/AdminAIModelController.java
Service:     application/admin/aimodel/AdminAIModelService.java
Entity:      domain/aimodel/entity/AIModel.java (공유)
Repository:  domain/aimodel/repository/AIModelRepository.java (공유)
DTOs:
  - application/admin/aimodel/dto/CreateAIModelRequest.java
  - application/admin/aimodel/dto/UpdateAIModelRequest.java

API Endpoints:
  - POST   /api/v1/admin/models           # 모델 생성 (관리자)
  - PUT    /api/v1/admin/models/{id}     # 모델 수정 (관리자)
  - DELETE /api/v1/admin/models/{id}     # 모델 삭제 (관리자)
```

### ChatRoom (채팅방)
```
Controller:  controller/chat/ChatRoomController.java
Service:     application/chatroom/ChatRoomService.java
Entity:      domain/chatroom/entity/ChatRoom.java
Repository:  domain/chatroom/repository/ChatRoomRepository.java
DTOs:
  - application/chatroom/dto/CreateChatRoomRequest.java
  - application/chatroom/dto/UpdateChatRoomRequest.java
  - application/chatroom/dto/ChatRoomResponse.java
  - application/chatroom/dto/ChatRoomListItemResponse.java

API Endpoints:
  - POST   /api/v1/chat-rooms              # 채팅방 생성
  - GET    /api/v1/chat-rooms              # 채팅방 목록 (페이지네이션)
  - GET    /api/v1/chat-rooms/{roomId}    # 채팅방 상세
  - PUT    /api/v1/chat-rooms/{roomId}    # 채팅방 수정
  - DELETE /api/v1/chat-rooms/{roomId}    # 채팅방 삭제
```

### Message (메시지)
```
Controller:  controller/chat/ChatMessageController.java
Service:     application/message/MessageService.java
Entity:      domain/message/entity/Message.java
Repository:  domain/message/repository/MessageRepository.java
DTOs:
  - application/message/dto/MessageResponse.java
  - application/message/dto/MessageListItemResponse.java

API Endpoints:
  - GET /api/v1/messages/page/{roomId}        # 메시지 목록 (페이지네이션)
  - GET /api/v1/messages/{messageId}          # 메시지 상세
```

### UserWallet (사용자 지갑)
```
Controller:  controller/userwallet/UserWalletController.java
Service:     application/userwallet/UserWalletService.java
Entity:      domain/userwallet/entity/UserWallet.java
Repository:  domain/userwallet/repository/UserWalletRepository.java
DTOs:
  - application/userwallet/dto/UserWalletResponse.java
  - application/userwallet/dto/BalanceResponse.java

API Endpoints:
  - GET /api/v1/wallet          # 지갑 상세 정보
  - GET /api/v1/wallet/balance  # 잔액 조회
```

### PaymentHistory (결제 내역)
```
Controller:  controller/paymenthistory/PaymentHistoryController.java
Service:     application/paymenthistory/PaymentHistoryService.java
Entity:      domain/paymenthistory/entity/PaymentHistory.java
Repository:  domain/paymenthistory/repository/PaymentHistoryRepository.java
DTOs:
  - application/paymenthistory/dto/PaymentResponse.java

API Endpoints:
  - GET /api/v1/payments              # 결제 목록 (status 필터)
  - GET /api/v1/payments/{paymentId} # 결제 상세
```

### CoinTransaction (코인 거래)
```
Controller:  controller/cointransaction/CoinTransactionController.java
Service:     application/cointransaction/CoinTransactionService.java
Entity:      domain/cointransaction/entity/CoinTransaction.java
Repository:  domain/cointransaction/repository/CoinTransactionRepository.java
DTOs:
  - application/cointransaction/dto/CoinTransactionResponse.java

API Endpoints:
  - GET /api/v1/transactions  # 거래 내역 (type, date 필터)
```

### Dashboard (대시보드)
```
Controller:  controller/dashboard/DashboardController.java
Service:     application/dashboard/DashboardService.java
DTOs:
  - application/dashboard/dto/ModelPricingResponse.java
  - application/dashboard/dto/MonthlyUsageResponse.java
  - application/dashboard/dto/UserStatsResponse.java
  - application/dashboard/dto/ModelUsageDetail.java
  - application/dashboard/dto/DailyUsageDetail.java
  - application/dashboard/dto/MostUsedModel.java

API Endpoints:
  - GET /api/v1/dashboard/models/pricing   # 모델 가격 정보 (Public)
  - GET /api/v1/dashboard/usage/monthly    # 월별 사용량 통계
  - GET /api/v1/dashboard/stats            # 사용자 통계 요약
```

### Auth (인증)
```
Controller:  controller/auth/
  - AuthController.java
  - TokenController.java
Service:     application/auth/
  - CustomOAuth2UserService.java
  - TokenHashService.java
  - accesstoken/AccessTokenService.java
  - refreshtoken/RefreshTokenService.java
Entity:      domain/accesstoken/entity/AccessToken.java
             domain/refreshtoken/entity/RefreshToken.java
Repository:  domain/accesstoken/repository/AccessTokenRepository.java
             domain/refreshtoken/repository/RefreshTokenRepository.java

API Endpoints:
  - POST /api/v1/auth/kakao      # Kakao OAuth2 로그인
  - POST /api/token/refresh      # 토큰 갱신
```

---

## 🔑 주요 컴포넌트 위치

### 인증 & 보안
```
JWT 토큰 프로바이더:    global/auth/jwt/JwtTokenProvider.java
JWT 인증 필터:          global/auth/jwt/JwtAuthenticationFilter.java
JWT 예외 핸들러:        global/auth/jwt/JwtAuthenticationEntryPoint.java
OAuth2 성공 핸들러:     global/auth/userinfo/OAuth2SuccessHandler.java
OAuth2 사용자 서비스:   application/auth/CustomOAuth2UserService.java
Security 설정:          global/config/SecurityConfig.java
SecurityContext 헬퍼:   global/auth/SecurityContextHelper.java ✨
```

### 공통 컴포넌트
```
API 응답 래퍼:          global/common/response/ApiResponse.java
전역 예외 핸들러:       global/error/GlobalExceptionHandler.java
에러 코드 정의:         global/error/ErrorCode.java
커스텀 예외:            global/error/exception/*Exception.java
```

### 설정 클래스
```
JPA 설정:              global/config/JpaConfig.java
OpenAPI/Swagger 설정:  global/config/OpenApiConfig.java
Security 설정:         global/config/SecurityConfig.java
```

### 애플리케이션 진입점
```
Main 클래스:           AiHubBeApplication.java
```

---

## ⚙️ 설정 파일

### application.yaml
**위치**: `src/main/resources/application.yaml`

```yaml
주요 설정:
  - spring.datasource: 데이터베이스 연결 (H2/PostgreSQL)
  - spring.jpa: JPA/Hibernate 설정
    - default_batch_fetch_size: 100 (N+1 해결)
  - spring.security.oauth2: Kakao OAuth2 설정
  - jwt: JWT 토큰 설정 (secret, expiration)
  - cors: CORS 허용 origin 설정
  - logging: 로깅 레벨 설정
```

### build.gradle
**위치**: `build.gradle`

```gradle
주요 의존성:
  - Spring Boot 3.5.6
  - Spring Security
  - Spring Data JPA
  - PostgreSQL Driver
  - H2 Database
  - Lombok
  - Validation
  - Springdoc OpenAPI (Swagger)
```

---

## 📐 코딩 컨벤션

### 네이밍 규칙

| 대상 | 규칙 | 예시 |
|------|------|------|
| 클래스 | PascalCase | `UserService`, `ChatRoomController` |
| 메서드 | camelCase | `getCurrentUser()`, `createChatRoom()` |
| 변수 | camelCase | `userId`, `chatRoom` |
| 상수 | UPPER_SNAKE_CASE | `DEFAULT_PAGE_SIZE` |
| 패키지 | lowercase | `application.user`, `domain.chatroom` |

### DTO 네이밍 패턴
```
Request DTO:   {Action}{Domain}Request
               예: CreateChatRoomRequest, UpdateUserRequest

Response DTO:  {Domain}Response, {Domain}ListItemResponse
               예: UserResponse, ChatRoomListItemResponse
```

### Service 메서드 네이밍
```
조회 (단건):    get{Domain}      예: getUser(), getChatRoom()
조회 (목록):    get{Domain}s     예: getChatRooms(), getMessages()
생성:          create{Domain}   예: createChatRoom()
수정:          update{Domain}   예: updateChatRoom()
삭제:          delete{Domain}   예: deleteChatRoom()
```

### 어노테이션 사용 패턴
```java
// Service 클래스
@Slf4j                              // 로깅
@Service                            // 스프링 빈 등록
@RequiredArgsConstructor            // 생성자 주입
@Transactional(readOnly = true)     // 클래스 레벨 (조회)
public class UserService {

    @Transactional                  // 메서드 레벨 (쓰기)
    public void updateUser() { }
}

// Controller 클래스
@RestController                     // REST API 컨트롤러
@RequestMapping("/api/v1/users")    // 기본 경로
@RequiredArgsConstructor
public class UserController {

    @GetMapping("/{userId}")        // HTTP 메서드 + 경로
    public ResponseEntity<ApiResponse<UserResponse>> getUser(
        @PathVariable Integer userId   // 경로 변수
    ) { }
}

// Entity 클래스
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class User { }
```

### 트랜잭션 관리
```java
// 기본: 클래스 레벨에 readOnly = true
@Transactional(readOnly = true)
public class UserService {

    // 조회 메서드는 별도 어노테이션 불필요
    public UserResponse getUser(Integer userId) { }

    // 쓰기 메서드만 @Transactional 오버라이드
    @Transactional
    public UserResponse updateUser(UpdateUserRequest request) { }
}
```

### 예외 처리 패턴
```java
// 도메인별 커스텀 예외 사용
throw new UserNotFoundException("사용자를 찾을 수 없습니다: " + userId);
throw new ForbiddenException("권한이 없습니다");
throw new ValidationException("잘못된 요청입니다");

// 로깅과 함께 사용
log.error("사용자를 찾을 수 없습니다: userId={}", userId);
throw new UserNotFoundException("사용자를 찾을 수 없습니다");
```

### 페이지네이션 패턴
```java
// Controller: 기본값 설정
@GetMapping
public ResponseEntity<ApiResponse<Page<ChatRoomListItemResponse>>> getChatRooms(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size,
    @RequestParam(defaultValue = "createdAt,desc") String sort
) {
    // sort 문자열 파싱
    String[] sortParams = sort.split(",");
    String sortField = sortParams[0];
    Sort.Direction direction = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("desc")
            ? Sort.Direction.DESC
            : Sort.Direction.ASC;

    Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));
    return ResponseEntity.ok(ApiResponse.ok(service.getChatRooms(pageable)));
}
```

### DTO 변환 패턴
```java
// Entity → Response DTO: 정적 팩토리 메서드
public record UserResponse(...) {
    public static UserResponse from(User user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .build();
    }
}

// 사용
UserResponse response = UserResponse.from(user);
```

---

## 🔍 빠른 검색 팁

### 특정 기능 찾기
1. **API 엔드포인트 찾기**: `spec/api.md` 참조
2. **도메인 클래스 찾기**: 위 "도메인별 파일 맵" 섹션 참조
3. **예외 처리 찾기**: `global/error/exception/` 확인
4. **설정 변경**: `src/main/resources/application.yaml` 확인

### 파일 경로 규칙
```
Controller:   controller/{domain}/{Domain}Controller.java
Service:      application/{domain}/{Domain}Service.java
Entity:       domain/{domain}/entity/{Domain}.java
Repository:   domain/{domain}/repository/{Domain}Repository.java
DTO:          application/{domain}/dto/{Name}.java
Exception:    global/error/exception/{Name}Exception.java
```

---

## 📚 관련 문서

- [API 명세서](./api.md)
- [데이터베이스 스키마](./database-schema.md) *(작성 예정)*
- [배포 가이드](./deployment.md) *(작성 예정)*

---

## 📌 최근 주요 변경사항

### 2025-11-17
- **컨트롤러 구조 개선**: 채팅 관련 컨트롤러를 `chat/` 패키지로 통합
  - `ChatRoomController`, `ChatMessageController`를 단일 패키지에서 관리
- **Swagger/OpenAPI 추가**: API 문서 자동 생성 설정 (springdoc-openapi-starter-webmvc-ui:2.8.13)
  - OpenAPI UI 접근: http://localhost:8080/swagger-ui.html
  - OpenAPI 스펙: http://localhost:8080/v3/api-docs
- **메시지 엔드포인트 변경**: `GET /api/v1/messages/page/{roomId}` 경로 변경
- **Security 설정 개선**: Swagger 경로 및 OPTIONS preflight 요청 허용

### 2025-11-11
- **SecurityContextHelper 추가**: 8개 서비스에서 중복되던 `getCurrentUserId()` 로직 공통화
- **Count 쿼리 최적화**: MessageRepository, ChatRoomRepository에 count 메서드 추가
- **UserWallet 검증 강화**: 도메인 엔티티에 잔액 검증 로직 추가
- **N+1 쿼리 해결**: Hibernate `default_batch_fetch_size: 100` 설정
- **CORS 설정 개선**: 환경변수 기반 설정으로 변경

---

**문서 버전**: 1.1.0
**작성자**: Claude Code
**마지막 업데이트**: 2025-11-17
