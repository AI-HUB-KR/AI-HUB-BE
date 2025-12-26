# 레이어 기반 → 도메인 기반 구조 리팩토링 계획

## 1. 현재 구조 분석

### 현재 레이어 기반 구조
```
src/main/java/kr/ai_hub/AI_HUB_BE/
├── controller/      # 표현 계층
│   ├── chat/
│   ├── auth/
│   ├── payment/
│   ├── aimodel/
│   ├── admin/
│   ├── user/
│   └── dashboard/
├── application/     # 응용 계층 (서비스)
│   ├── chat/
│   ├── auth/
│   ├── payment/
│   ├── aimodel/
│   ├── admin/
│   ├── user/
│   ├── dashboard/
│   └── userwallet/
├── domain/          # 도메인 계층
│   ├── chat/
│   ├── auth/
│   ├── payment/
│   ├── aimodel/
│   └── user/
└── global/          # 공통 모듈
    ├── config/
    ├── auth/
    ├── common/
    ├── application/
    └── error/
```

### 식별된 도메인
1. **chat** - 채팅방, 메시지 관리
2. **auth** - 인증, 토큰 관리
3. **payment** - 결제 내역, 코인 거래
4. **aimodel** - AI 모델 정보
5. **user** - 사용자, 사용자 지갑
6. **admin** - 관리자 기능 (AI 모델 관리, 사용자 관리)
7. **dashboard** - 통계 및 대시보드
8. **global** - 공통 모듈 (config, error, common 등)

## 2. 최종 확정된 도메인 기반 구조치 ✅

### 도메인 구조
```
src/main/java/kr/ai_hub/AI_HUB_BE/
├── chat/
│   ├── controller/
│   │   ├── ChatRoomController.java
│   │   └── ChatMessageController.java
│   ├── service/
│   │   ├── ChatRoomService.java
│   │   ├── MessageService.java
│   │   ├── MessageTransactionService.java
│   │   ├── FileValidationService.java
│   │   ├── FileUploadService.java
│   │   ├── MessageRequestBuilder.java
│   │   └── AiSseHandler.java
│   ├── domain/
│   │   ├── ChatRoom.java
│   │   ├── Message.java
│   │   ├── MessageRole.java
│   │   ├── ChatRoomRepository.java
│   │   └── MessageRepository.java
│   └── dto/
│       ├── (chatroom, message 관련 DTO들)
│
├── auth/
│   ├── controller/
│   │   ├── AuthController.java
│   │   └── TokenController.java
│   ├── service/
│   │   ├── CustomOAuth2UserService.java
│   │   ├── RefreshTokenService.java
│   │   ├── AccessTokenService.java
│   │   └── TokenHashService.java
│   ├── domain/
│   │   ├── RefreshToken.java
│   │   ├── AccessToken.java
│   │   ├── TokenRevokeReason.java
│   │   ├── RefreshTokenRepository.java
│   │   └── AccessTokenRepository.java
│   └── dto/
│
├── wallet/                    # 💰 모든 지갑 관련 기능 통합
│   ├── controller/
│   │   ├── UserWalletController.java
│   │   ├── WalletHistoryController.java
│   │   └── CoinTransactionController.java
│   ├── service/
│   │   ├── UserWalletService.java
│   │   ├── WalletHistoryService.java
│   │   └── CoinTransactionService.java
│   ├── domain/
│   │   ├── UserWallet.java
│   │   ├── WalletHistory.java
│   │   ├── WalletHistoryType.java
│   │   ├── CoinTransaction.java
│   │   ├── UserWalletRepository.java
│   │   ├── WalletHistoryRepository.java
│   │   └── CoinTransactionRepository.java
│   └── dto/
│       ├── UserWalletResponse.java
│       ├── BalanceResponse.java
│       ├── PaymentResponse.java
│       └── CoinTransactionResponse.java
│
├── aimodel/
│   ├── controller/
│   │   └── AIModelController.java
│   ├── service/
│   │   └── AIModelService.java
│   ├── domain/
│   │   ├── AIModel.java
│   │   └── AIModelRepository.java
│   └── dto/
│
├── user/
│   ├── controller/
│   │   └── UserController.java
│   ├── service/
│   │   └── UserService.java
│   ├── domain/
│   │   ├── User.java
│   │   ├── UserRole.java
│   │   └── UserRepository.java
│   └── dto/
│
├── admin/                     # 🔒 관리자 전용 도메인
│   ├── controller/
│   │   ├── AdminUserController.java
│   │   └── AdminAIModelController.java
│   ├── service/
│   │   ├── AdminUserService.java
│   │   └── AdminAIModelService.java
│   └── dto/
│       ├── ModifyUserAuthorityRequest.java
│       ├── ModifyUserWalletRequest.java
│       ├── UserListResponse.java
│       ├── CreateAIModelRequest.java
│       └── UpdateAIModelRequest.java
│
├── dashboard/
│   ├── controller/
│   │   └── DashboardController.java
│   ├── service/
│   │   └── DashboardService.java
│   └── dto/
│
└── global/                    # 공통 모듈
    ├── config/
    │   ├── SecurityConfig.java
    │   ├── OpenApiConfig.java
    │   ├── JpaAuditingConfig.java
    │   └── RestClientConfig.java
    ├── security/              # 🔐 보안 관련 통합
    │   ├── jwt/
    │   │   ├── JwtTokenProvider.java
    │   │   ├── JwtAuthenticationFilter.java
    │   │   └── JwtAuthenticationEntryPoint.java
    │   ├── oauth2/
    │   │   ├── CustomOauth2User.java
    │   │   ├── CustomAuthenticationSuccessHandler.java
    │   │   ├── OAuth2UserInfo.java
    │   │   ├── OAuth2UserInfoFactory.java
    │   │   └── KakaoOAuth2UserInfo.java
    │   └── SecurityContextHelper.java
    ├── common/
    │   ├── response/
    │   │   ├── ApiResponse.java
    │   │   └── ErrorResponse.java
    │   └── CookieService.java
    └── error/
        ├── ErrorCode.java
        ├── GlobalExceptionHandler.java
        └── exception/
            ├── BaseException.java
            └── (기타 예외 클래스들)
```

### 주요 변경사항
1. **wallet 도메인 신규 생성**: UserWallet, WalletHistory, CoinTransaction 통합
2. **payment 도메인 삭제**: 추후 외부 결제 게이트웨이 연동 시 재생성 예정
3. **admin 도메인 별도 분리**: 관리자 전용 기능 집중화
4. **global/security 생성**: 기존 global/auth를 security로 재구성 (JWT, OAuth2 통합)
5. **서비스 레이어**: `application` → `service`로 네이밍 변경
6. **DTO 위치**: 각 도메인 내부에 `dto/` 패키지 배치

## 3. 확정된 설계 결정사항 ✅

1. **Admin 구조**: ✅ 별도 도메인으로 분리 (관리자 전용 기능 집중화)
2. **UserWallet**: ✅ wallet 도메인 신규 생성 (UserWallet, WalletHistory, CoinTransaction 통합)
3. **Dashboard**: ✅ 별도 도메인 유지 (여러 도메인 데이터 집계)
4. **서비스 레이어**: ✅ `application` → `service`로 변경
5. **DTO 구조**: ✅ 각 도메인 내부에 `dto/` 패키지
6. **Global 패키지**: ✅ `global/security/`로 JWT, OAuth2 통합

## 4. 예상 사이드 이펙트

### 4.1 Import 문 변경
- **영향 범위**: 모든 Java 파일 (~150개 파일)
- **리스크**: 컴파일 오류 가능성
- **대응**: IDE의 자동 import 정리 기능 활용, 단계적 이동

### 4.2 패키지 순환 참조
- **리스크**: 도메인 간 의존성으로 인한 순환 참조 발생 가능
- **대응**: 의존성 분석 → 인터페이스 분리 또는 이벤트 기반 통신

### 4.3 테스트 코드 동기화
- **영향 범위**: 모든 테스트 파일 (~45개 파일)
- **리스크**: 테스트 실패, import 오류
- **대응**: 프로덕션 코드 이동 후 즉시 테스트 코드 동기화

### 4.4 Spring Bean 스캔
- **리스크**: Component Scan 범위 변경으로 빈 등록 누락 가능
- **대응**: `@ComponentScan` basePackages 확인, 통합 테스트로 검증

### 4.5 JPA Repository 경로
- **리스크**: Repository 인터페이스 경로 변경으로 `@EnableJpaRepositories` 설정 필요 가능
- **대응**: Repository 경로 확인 및 설정 업데이트

### 4.6 OpenAPI/Swagger 설정
- **리스크**: Controller 경로 변경으로 API 문서 스캔 범위 조정 필요
- **대응**: OpenApiConfig의 패키지 스캔 범위 업데이트

### 4.7 보안 설정
- **리스크**: SecurityConfig에서 경로 기반 권한 설정이 있을 경우 영향
- **대응**: 경로 기반 설정 확인 및 업데이트

## 5. 마이그레이션 실행 계획

### 5.1 도메인별 마이그레이션 순서 (의존성 기반)
```
1. user        (기본 도메인, 다른 도메인이 User 엔티티 참조)
2. aimodel     (독립적)
3. auth        (User 의존)
4. wallet      (User 의존) - 신규 생성
5. chat        (User, AIModel 의존)
6. admin       (User, AIModel 의존)
7. dashboard   (모든 도메인 데이터 집계)
8. global      (공통 모듈 재구성)
```

### 5.2 각 도메인별 작업 단계
각 도메인마다 다음 순서로 진행:
1. **폴더 구조 생성**: `controller/`, `service/`, `domain/`, `dto/`
2. **파일 이동**: 기존 파일을 새 구조로 이동
3. **패키지 선언 수정**: 각 파일의 `package` 선언 업데이트
4. **Import 문 수정**: 변경된 패키지 경로로 import 업데이트
5. **테스트 코드 동기화**: 해당 도메인의 테스트 코드도 동일하게 처리
6. **컴파일 검증**: `./gradlew compileJava` 실행
7. **테스트 실행**: 해당 도메인 테스트 실행

### 5.3 병렬 처리 전략
- **독립적인 도메인**: user, aimodel은 병렬 처리 가능
- **Subagent 활용**: 각 도메인별로 별도 agent 할당하여 동시 처리
- **테스트 코드**: 프로덕션 코드 이동 후 즉시 병렬로 테스트 코드 이동

### 5.4 특수 케이스 처리

#### wallet 도메인 (신규 생성)
- **기존 위치**:
  - `domain/user/UserWallet.java` → `wallet/domain/`
  - `domain/payment/WalletHistory.java` → `wallet/domain/`
  - `domain/payment/CoinTransaction.java` → `wallet/domain/`
  - `application/userwallet/` → `wallet/service/`
  - `application/payment/WalletHistoryService.java` → `wallet/service/`
  - `application/payment/CoinTransactionService.java` → `wallet/service/`
  - `controller/user/UserWalletController.java` → `wallet/controller/`
  - `controller/payment/WalletHistoryController.java` → `wallet/controller/`
  - `controller/payment/CoinTransactionController.java` → `wallet/controller/`

#### global/security (재구성)
- **기존 `global/auth/`** → `global/security/`로 이동
  - `jwt/` → `security/jwt/`
  - `userinfo/` → `security/oauth2/`로 변경
  - 기타 OAuth2 관련 파일 → `security/oauth2/`

#### payment 도메인 삭제
- wallet으로 이동하지 않은 파일이 있는지 확인
- 모든 파일 이동 완료 후 빈 폴더 삭제

### 5.5 롤백 전략
- Git feature branch: `feat/folder-refactor`
- 각 도메인 마이그레이션 후 커밋
- 커밋 메시지: "refactor: migrate {domain} to domain-based structure"
- 문제 발생 시 해당 도메인 커밋만 revert

## 6. 검증 체크리스트

### 빌드 검증
- [ ] `./gradlew compileJava` 성공
- [ ] `./gradlew compileTestJava` 성공
- [ ] `./gradlew build` 성공

### 테스트 검증
- [ ] 단위 테스트 전체 통과
- [ ] 통합 테스트 전체 통과
- [ ] Repository 테스트 통과

### 기능 검증
- [ ] Spring Boot 애플리케이션 정상 기동
- [ ] Swagger UI 정상 접근 (`/swagger-ui.html`)
- [ ] API 엔드포인트 정상 동작

### 문서 업데이트
- [ ] CLAUDE.md 업데이트
- [ ] spec/codebase-structure.md 업데이트
- [ ] 기타 관련 문서 업데이트

## 7. 실행 준비 완료 ✅

### 확정된 사항
- [x] 도메인 구조 확정
- [x] wallet 도메인 설계 완료
- [x] 마이그레이션 순서 결정
- [x] 병렬 처리 전략 수립
- [x] 특수 케이스 처리 방안 수립

### 다음 단계
1. ✅ Git feature branch 생성
2. 🔄 도메인별 마이그레이션 실행 (순차 or 병렬)
3. 🔄 테스트 코드 동기화
4. 🔄 빌드 및 테스트 검증
5. 🔄 문서 업데이트 (CLAUDE.md, spec/codebase-structure.md)
6. 🔄 커밋 및 PR 생성
