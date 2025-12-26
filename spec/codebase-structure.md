# AI-HUB-BE 코드베이스 구조 가이드

> 이 문서는 코드베이스 탐색 시 빠르게 필요한 파일을 찾을 수 있도록 작성된 구조 가이드입니다.
>
> 마지막 업데이트: 2025-12-26
>
> **주요 변경사항**: 레이어 기반 구조에서 도메인 기반 구조로 마이그레이션 완료

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
**아키텍처**: Domain-Driven Design (도메인 중심 아키텍처)
**패키지 전략**: Package by Feature - 도메인별 계층화 구조 (Domain/{controller|service|domain|dto})

### 통계
- **전체 Java 파일**: 123개
- **Entity**: 9개
- **Repository**: 9개
- **Service**: 17개
- **Controller**: 12개
- **DTO**: 34개

---

## 🏗 아키텍처 패턴

### 도메인 중심 구조 (Domain-Based Architecture)
```
src/main/java/kr/ai_hub/AI_HUB_BE/
├── {domain}/                     ← 도메인별 독립적인 패키지
│   ├── controller/               ← HTTP 요청/응답 처리 (Presentation)
│   ├── service/                  ← 비즈니스 로직, 트랜잭션 (Application)
│   ├── domain/                   ← 엔티티, 리포지토리 (Domain)
│   └── dto/                      ← 데이터 전송 객체
└── global/                       ← 전역 공통 모듈
    ├── config/                   ← 설정
    ├── security/                 ← 보안, 인증
    ├── common/                   ← 공통 유틸
    └── error/                    ← 에러 처리

7개 주요 도메인: user, aimodel, auth, wallet, chat, admin, dashboard
```

### 도메인 내부 계층 구조 (각 도메인별로 동일)
```
{domain}/
├── controller/     ← Presentation Layer
│   └── *Controller.java
├── service/        ← Application Layer
│   └── *Service.java
├── domain/         ← Domain Layer
│   ├── *.java      (Entity)
│   └── *Repository.java
└── dto/            ← Data Transfer Objects
    ├── *Request.java
    └── *Response.java
```

### 레이어 간 의존성 규칙
- Controller → Service (O)
- Service → Repository (O)
- Service → 다른 도메인의 Service (O) - 예: UserWalletService → UserService
- Repository → Entity (O)
- **역방향 의존성 금지** (하위 레이어가 상위 레이어 참조 X)
- **도메인 격리 원칙**: 각 도메인은 독립적이며, 필요시 Service 레이어를 통해 상호작용

---

## 📁 도메인별 구조

### 1. User 도메인
**위치**: `src/main/java/kr/ai_hub/AI_HUB_BE/user/`

```
user/
├── controller/
│   └── UserController.java                      # 사용자 정보 API
├── service/
│   └── UserService.java                         # 사용자 정보 서비스
├── domain/
│   ├── User.java                                # 사용자 엔티티
│   ├── UserRepository.java
│   └── UserRole.java                            # Enum: ROLE_USER, ROLE_ADMIN
└── dto/
    ├── UpdateUserRequest.java
    └── UserResponse.java
```

### 2. AI Model 도메인
**위치**: `src/main/java/kr/ai_hub/AI_HUB_BE/aimodel/`

```
aimodel/
├── controller/
│   └── AIModelController.java                   # AI 모델 조회 API
├── service/
│   └── AIModelService.java                      # AI 모델 조회 서비스
├── domain/
│   ├── AIModel.java                             # AI 모델 엔티티
│   └── AIModelRepository.java
└── dto/
    ├── AIModelResponse.java
    └── AIModelDetailResponse.java
```

### 3. Auth 도메인
**위치**: `src/main/java/kr/ai_hub/AI_HUB_BE/auth/`

```
auth/
├── controller/
│   ├── AuthController.java                      # OAuth2 로그인 API
│   └── TokenController.java                     # 토큰 갱신 API
├── service/
│   ├── CustomOAuth2UserService.java             # OAuth2 사용자 서비스
│   ├── TokenHashService.java                    # 토큰 해싱 서비스
│   ├── AccessTokenService.java                  # Access Token 서비스
│   └── RefreshTokenService.java                 # Refresh Token 서비스
├── domain/
│   ├── AccessToken.java                         # Access Token 엔티티
│   ├── AccessTokenRepository.java
│   ├── RefreshToken.java                        # Refresh Token 엔티티
│   ├── RefreshTokenRepository.java
│   └── TokenRevokeReason.java                   # Token 폐기 이유 Enum
└── dto/
    ├── RefreshedTokens.java
    └── TokenRefreshResponse.java
```

### 4. Wallet 도메인
**위치**: `src/main/java/kr/ai_hub/AI_HUB_BE/wallet/`

```
wallet/
├── controller/
│   ├── UserWalletController.java                # 사용자 지갑 API
│   ├── CoinTransactionController.java           # 코인 거래 내역 API
│   └── WalletHistoryController.java             # 지갑 이력 API
├── service/
│   ├── UserWalletService.java                   # 사용자 지갑 서비스
│   ├── CoinTransactionService.java              # 코인 거래 내역 서비스
│   └── WalletHistoryService.java                # 지갑 이력 서비스
├── domain/
│   ├── UserWallet.java                          # 사용자 지갑 엔티티
│   ├── UserWalletRepository.java
│   ├── CoinTransaction.java                     # 코인 거래 엔티티
│   ├── CoinTransactionRepository.java
│   ├── WalletHistory.java                       # 지갑 이력 엔티티
│   ├── WalletHistoryRepository.java
│   └── WalletHistoryType.java                   # 지갑 이력 타입 Enum
└── dto/
    ├── UserWalletResponse.java
    ├── BalanceResponse.java
    ├── CoinTransactionResponse.java
    └── WalletHistoryResponse.java
```

### 5. Chat 도메인
**위치**: `src/main/java/kr/ai_hub/AI_HUB_BE/chat/`

```
chat/
├── controller/
│   ├── ChatRoomController.java                  # 채팅방 CRUD API
│   └── ChatMessageController.java               # 메시지 조회/전송 API
├── service/
│   ├── ChatRoomService.java                     # 채팅방 CRUD 서비스
│   ├── MessageService.java                      # 메시지 오케스트레이션
│   ├── MessageTransactionService.java           # 메시지 저장/정산 트랜잭션
│   ├── AiSseHandler.java                        # AI 서버 SSE 스트리밍
│   ├── FileValidationService.java               # 파일 검증
│   ├── FileUploadService.java                   # AI 서버 파일 업로드
│   └── MessageRequestBuilder.java               # AI 요청 바디 빌더
├── domain/
│   ├── ChatRoom.java                            # 채팅방 엔티티 (UUID v7)
│   ├── ChatRoomRepository.java
│   ├── Message.java                             # 메시지 엔티티 (UUID v7)
│   ├── MessageRepository.java
│   └── MessageRole.java                         # Enum: USER, ASSISTANT
└── dto/
    ├── CreateChatRoomRequest.java
    ├── UpdateChatRoomRequest.java
    ├── ChatRoomResponse.java
    ├── ChatRoomListItemResponse.java
    ├── SendMessageRequest.java
    ├── MessageResponse.java
    ├── MessageListItemResponse.java
    ├── FileUploadResponse.java
    ├── ChatHistoryMessage.java
    ├── FileType.java
    ├── AiServerResponse.java
    ├── AiUploadData.java
    ├── AiChatData.java
    ├── AiUsage.java
    ├── AiStreamingResult.java
    └── SseEvent.java
```

### 6. Admin 도메인
**위치**: `src/main/java/kr/ai_hub/AI_HUB_BE/admin/`

```
admin/
├── controller/
│   ├── AdminAIModelController.java              # 관리자 AI 모델 관리 API
│   └── AdminUserController.java                 # 관리자 사용자 관리 API
├── service/
│   ├── AdminAIModelService.java                 # 관리자 AI 모델 서비스
│   └── AdminUserService.java                    # 관리자 사용자 서비스
└── dto/
    ├── CreateAIModelRequest.java
    ├── UpdateAIModelRequest.java
    ├── UpdateUserRoleRequest.java
    └── AdminWalletModifyRequest.java
```

### 7. Dashboard 도메인
**위치**: `src/main/java/kr/ai_hub/AI_HUB_BE/dashboard/`

```
dashboard/
├── controller/
│   └── DashboardController.java                 # 대시보드 통계 API
├── service/
│   └── DashboardService.java                    # 대시보드 통계 서비스
└── dto/
    ├── ModelPricingResponse.java
    ├── MonthlyUsageResponse.java
    ├── UserStatsResponse.java
    ├── ModelUsageDetail.java
    ├── DailyUsageDetail.java
    └── MostUsedModel.java
```

### 8. Global 모듈
**위치**: `src/main/java/kr/ai_hub/AI_HUB_BE/global/`

```
global/
├── security/
│   ├── SecurityContextHelper.java               # 보안 컨텍스트 헬퍼
│   ├── CustomAuthenticationSuccessHandler.java  # OAuth2 성공 핸들러
│   ├── oauth2/
│   │   ├── CustomOAuth2User.java                # OAuth2 사용자 Principal
│   │   ├── OAuth2UserInfoFactory.java
│   │   ├── KakaoOAuth2UserInfo.java
│   │   └── OAuth2UserInfo.java
│   └── jwt/
│       ├── JwtAuthenticationEntryPoint.java     # JWT 인증 실패 핸들러
│       ├── JwtAuthenticationFilter.java         # JWT 인증 필터
│       └── JwtTokenProvider.java                # JWT 토큰 생성/검증
├── common/
│   ├── CookieService.java                       # 토큰 쿠키 유틸
│   └── response/
│       ├── ApiResponse.java                     # 공통 API 응답 래퍼
│       └── ErrorResponse.java                   # 공통 에러 응답
├── config/
│   ├── JpaAuditingConfig.java                   # JPA 설정 (Auditing)
│   ├── OpenApiConfig.java                       # Swagger/OpenAPI 설정
│   ├── SecurityConfig.java                      # Spring Security 설정
│   └── RestClientConfig.java                    # RestClient 설정
└── error/
    ├── ErrorCode.java                           # 에러 코드 Enum
    ├── GlobalExceptionHandler.java              # 전역 예외 핸들러
    └── exception/
        ├── BaseException.java                   # 기본 예외 클래스
        ├── AIServerException.java
        └── ... (도메인별 예외)
```

**역할**: 전역 설정, 공통 컴포넌트, 횡단 관심사(인증, 예외처리)

---

## 🗂 도메인별 파일 맵

### User (사용자)
```
Controller:  controller/user/UserController.java
Service:     application/user/UserService.java
Entity:      domain/user/User.java
Repository:  domain/user/UserRepository.java
DTOs:
  - application/user/dto/UpdateUserRequest.java
  - application/user/dto/UserResponse.java

API Endpoints:
  - GET    /api/v1/users/me           # 내 정보 조회
  - PUT    /api/v1/users/me           # 내 정보 수정
  - DELETE /api/v1/users/me           # 회원 탈퇴
```

### UserWallet (사용자 지갑)
```
Controller:  controller/user/UserWalletController.java
Service:     application/userwallet/UserWalletService.java
Entity:      domain/user/UserWallet.java
Repository:  domain/user/UserWalletRepository.java
DTOs:
  - application/userwallet/dto/UserWalletResponse.java
  - application/userwallet/dto/BalanceResponse.java

API Endpoints:
  - GET /api/v1/wallet          # 지갑 상세 정보
  - GET /api/v1/wallet/balance  # 잔액 조회
```

### AIModel (AI 모델)
```
Controller:  controller/aimodel/AIModelController.java
Service:     application/aimodel/AIModelService.java
Entity:      domain/aimodel/AIModel.java
Repository:  domain/aimodel/AIModelRepository.java
DTOs:
  - application/aimodel/dto/AIModelResponse.java

API Endpoints:
  - GET /api/v1/models              # 활성 모델 목록
```

### Admin AIModel (관리자 AI 모델 관리)
```
Controller:  controller/admin/AdminAIModelController.java
Service:     application/admin/AdminAIModelService.java
Entity:      domain/aimodel/AIModel.java (공유)
Repository:  domain/aimodel/AIModelRepository.java (공유)
DTOs:
  - application/admin/dto/CreateAIModelRequest.java
  - application/admin/dto/UpdateAIModelRequest.java
  - application/aimodel/dto/AIModelResponse.java
  - application/aimodel/dto/AIModelDetailResponse.java

API Endpoints:
  - POST   /api/v1/admin/models           # 모델 생성 (관리자)
  - GET    /api/v1/admin/models/{modelId}     # 모델 상세 조회 (관리자)
  - PUT    /api/v1/admin/models/{modelId}     # 모델 수정 (관리자)
  - DELETE /api/v1/admin/models/{modelId}     # 모델 삭제 (관리자)
```

### Admin Wallet (관리자 지갑 잔액 수정)
```
Controller:  controller/admin/AdminWalletModifyController.java
Service:     application/admin/AdminWalletModifyService.java
Entity:      domain/user/UserWallet.java
Repository:  domain/user/UserWalletRepository.java

API Endpoints:
  - PATCH /api/v1/admin/wallet  # userId, amount (query params)
```

### Chat (채팅)
```
Controller:  controller/chat/ChatRoomController.java
             controller/chat/ChatMessageController.java
Service:     application/chat/chatroom/ChatRoomService.java
             application/chat/message/MessageService.java
Entity:      domain/chat/ChatRoom.java
             domain/chat/Message.java
             domain/chat/MessageRole.java (Enum)
Repository:  domain/chat/ChatRoomRepository.java
             domain/chat/MessageRepository.java
DTOs:
  - application/chat/chatroom/dto/CreateChatRoomRequest.java
  - application/chat/chatroom/dto/UpdateChatRoomRequest.java
  - application/chat/chatroom/dto/ChatRoomResponse.java
  - application/chat/chatroom/dto/ChatRoomListItemResponse.java
  - application/chat/message/dto/MessageResponse.java
  - application/chat/message/dto/MessageListItemResponse.java
  - application/chat/message/dto/FileUploadResponse.java
  - application/chat/message/dto/SendMessageRequest.java
  - application/chat/message/dto/AiServerResponse.java
  - application/chat/message/dto/AiUploadData.java
  - application/chat/message/dto/AiChatData.java
  - application/chat/message/dto/AiUsage.java
  - application/chat/message/dto/SseEvent.java

API Endpoints:
  - POST   /api/v1/chat-rooms              # 채팅방 생성
  - GET    /api/v1/chat-rooms              # 채팅방 목록 (페이지네이션)
  - GET    /api/v1/chat-rooms/{roomId}    # 채팅방 상세
  - PUT    /api/v1/chat-rooms/{roomId}    # 채팅방 수정
  - DELETE /api/v1/chat-rooms/{roomId}    # 채팅방 삭제
  - GET  /api/v1/messages/page/{roomId}                # 메시지 목록 (페이지네이션)
  - GET  /api/v1/messages/{messageId}                  # 메시지 상세
  - POST /api/v1/messages/files/upload                 # 파일 업로드 (AI 서버)
  - POST /api/v1/messages/send/{roomId} (text/event-stream) # 메시지 전송 (SSE)
```

### Payment (결제 및 코인)
```
Controller:  controller/payment/WalletHistoryController.java
             controller/payment/CoinTransactionController.java
Service:     application/payment/WalletHistoryService.java
             application/payment/CoinTransactionService.java
Entity:      domain/payment/WalletHistory.java
             domain/payment/CoinTransaction.java
Repository:  domain/payment/WalletHistoryRepository.java
             domain/payment/CoinTransactionRepository.java
DTOs:
  - application/payment/dto/PaymentResponse.java
  - application/payment/dto/CoinTransactionResponse.java

API Endpoints:
  - GET /api/v1/payments              # 결제 목록 (status 필터)
  - GET /api/v1/payments/{paymentId} # 지갑 이력 상세
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
  - GET /api/v1/dashboard/models/pricing   # 모델 가격 정보 (인증 필요)
  - GET /api/v1/dashboard/usage/monthly    # 월별 사용량 통계
  - GET /api/v1/dashboard/stats            # 사용자 통계 요약
```

### Auth (인증)
```
Controller:  controller/auth/AuthController.java
             controller/auth/TokenController.java
Service:     application/auth/CustomOAuth2UserService.java
             application/auth/TokenHashService.java
             application/auth/accesstoken/AccessTokenService.java
             application/auth/refreshtoken/RefreshTokenService.java
Entity:      domain/auth/AccessToken.java
             domain/auth/RefreshToken.java
Repository:  domain/auth/AccessTokenRepository.java
             domain/auth/RefreshTokenRepository.java

API Endpoints:
  - GET  /oauth2/authorization/kakao  # Kakao OAuth2 로그인 시작(리다이렉트)
  - POST /api/v1/token/refresh        # 토큰 갱신 (Refresh Token 쿠키 기반)
  - POST /api/v1/auth/logout          # 로그아웃
```

---

## 🔑 주요 컴포넌트 위치

### 인증 & 보안
```
JWT 토큰 프로바이더:    global/auth/jwt/JwtTokenProvider.java
JWT 인증 필터:          global/auth/jwt/JwtAuthenticationFilter.java
JWT 예외 핸들러:        global/auth/jwt/JwtAuthenticationEntryPoint.java
OAuth2 성공 핸들러:     global/auth/CustomAuthenticationSuccessHandler.java
OAuth2 사용자 서비스:   application/auth/CustomOAuth2UserService.java
Security 설정:          global/config/SecurityConfig.java
SecurityContext 헬퍼:   global/auth/SecurityContextHelper.java
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
JPA 설정:              global/config/JpaAuditingConfig.java
OpenAPI/Swagger 설정:  global/config/OpenApiConfig.java
Security 설정:         global/config/SecurityConfig.java
RestClient 설정:       global/config/RestClientConfig.java
```

### 애플리케이션 진입점
```
Main 클래스:           AiHubBeApplication.java
```

---

## ⚙️ 설정 파일

### application.yaml
**위치**: `src/main/resources/application.yaml`

### build.gradle
**위치**: `build.gradle`

---

## 📐 코딩 컨벤션

### 네이밍 규칙

| 대상 | 규칙 | 예시 |
|------|------|------|
| 클래스 | PascalCase | `UserService`, `ChatRoomController` |
| 메서드 | camelCase | `getCurrentUser()`, `createChatRoom()` |
| 변수 | camelCase | `userId`, `chatRoom` |
| 상수 | UPPER_SNAKE_CASE | `DEFAULT_PAGE_SIZE` |
| 패키지 | lowercase | `application.user`, `domain.chat` |

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

    @GetMapping("/me")              // HTTP 메서드 + 경로
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() { }
}

// Entity 클래스
@Entity
@Table(name = "\"user\"")
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
Controller:   {domain}/controller/*Controller.java
Service:      {domain}/service/*Service.java
Entity:       {domain}/domain/*.java
Repository:   {domain}/domain/*Repository.java
DTO:          {domain}/dto/*.java
Exception:    global/error/exception/*Exception.java
```

### 도메인 간 의존성 패턴
```java
// 예: UserWalletService → UserService (다른 도메인 Service 호출)
package kr.ai_hub.AI_HUB_BE.wallet.service;

import kr.ai_hub.AI_HUB_BE.user.service.UserService;  // 다른 도메인의 Service
import kr.ai_hub.AI_HUB_BE.wallet.domain.UserWallet;  // 자신의 도메인 Entity

@Service
public class UserWalletService {
    private final UserService userService;  // 의존성 주입
    // ...
}
```

---

## 📚 관련 문서

- [API 명세서](./api.md)
- [데이터베이스 스키마](./db.md)
- [AI 서버 연동](./msa-ai-server.md)
- [배포 가이드](./deployment.md) *(작성 예정)*

---

**문서 버전**: 2.0.0
**작성자**: Claude Code
**마지막 업데이트**: 2025-12-26
**주요 변경사항**: 레이어 기반 구조 → 도메인 기반 구조 마이그레이션 완료
