# AI Hub Backend

AI Hub는 여러 AI API(OpenAI, Anthropic, Google AI 등)를 코인 기반 선불제로 통합 제공하는 플랫폼의 백엔드 서버입니다.

## 주요 기능

- **OAuth2 소셜 로그인**: 카카오 소셜 로그인 지원
- **JWT 토큰 인증**: Access Token + Refresh Token 기반 인증/인가
- **채팅방 관리**: 사용자별 AI 채팅방 생성 및 관리
- **AI 메시지 전송**: SSE(Server-Sent Events) 기반 실시간 스트리밍 응답
- **코인 시스템**: 선불제 코인 충전 및 AI API 사용량 기반 차감
- **사용자 지갑**: 코인 잔액 관리 및 거래 내역 조회
- **대시보드**: 사용량 통계 및 모델별 사용 현황 제공
- **관리자 기능**: AI 모델 등록 및 가격 관리

## Tech Stack

### Framework & Language
- **Spring Boot**: 3.5.6
- **Java**: 25 (Eclipse Temurin)
- **Build Tool**: Gradle

### Core Dependencies
- **Spring Web**: Virtual Threads 활성화
- **Spring Data JPA**: Hibernate 기반 ORM
- **Spring Security**: OAuth2 Client + JWT 인증
- **Spring Actuator**: Health check 및 모니터링
- **WebFlux**: AI 서버 HTTP 클라이언트용 (WebClient만 사용)

### Database
- **PostgreSQL**: 운영 환경 데이터베이스
- **H2**: 개발/테스트 환경 인메모리 데이터베이스

### Security & Authentication
- **JWT**: JJWT 0.13.0 (Access Token + Refresh Token)
- **OAuth2**: 카카오 소셜 로그인

### API Documentation
- **Swagger/OpenAPI**: springdoc-openapi-starter-webmvc-ui 2.8.13
  - Swagger UI: `http://localhost:8080/swagger-ui.html` (개발 환경)
  - API Docs: `http://localhost:8080/v3/api-docs`

### Utilities
- **Lombok**: 보일러플레이트 코드 제거

## Getting Started

### Prerequisites

- **JDK 25** (Eclipse Temurin 권장)
- **PostgreSQL** (운영 환경) 또는 H2 (개발 환경 자동 설정)
- **카카오 개발자 계정** (OAuth2 클라이언트 ID/Secret)

### 환경 변수 설정

#### 필수 환경 변수 (운영 환경)

```bash
# 데이터베이스 설정
export DB_URL="jdbc:postgresql://localhost:5432/aihub"
export DB_USERNAME="your_db_username"
export DB_PASSWORD="your_db_password"

# OAuth2 소셜 로그인 (카카오)
export KAKAO_CLIENT_ID="your_kakao_client_id"
export KAKAO_CLIENT_SECRET="your_kakao_client_secret"

# JWT 토큰 설정
export JWT_SECRET="your_jwt_secret_key_minimum_256_bits"
export JWT_EXPIRATION_SECOND=3600           # Access Token 만료 시간 (초)
export JWT_REFRESH_EXPIRATION_SECOND=2592000  # Refresh Token 만료 시간 (초, 30일)

# 배포 주소
export DEPLOYMENT_ADDRESS="https://api.yourdomain.com"
export FRONTEND_ADDRESS="https://yourdomain.com"

# CORS 설정
export CORS_ALLOWED_ORIGINS="https://yourdomain.com,https://www.yourdomain.com"

# AI 서버 URL
export AI_SERVER_URL="http://ai-server:3000"

# Swagger 활성화 여부 (운영 환경에서는 false 권장)
export SWAGGER_ENABLED=false
```

#### 개발 환경 기본값

개발 환경(`-Dspring.profiles.active=dev`)에서는 대부분의 환경 변수에 기본값이 설정되어 있습니다:
- DB: H2 인메모리 데이터베이스 (자동 설정)
- JWT Secret: 개발용 시크릿 (프로덕션 사용 금지)
- CORS: `http://localhost:3000`, `http://localhost:5173`, `http://localhost:8080`
- AI Server: `http://localhost:3000`

개발 환경에서도 **카카오 OAuth2 설정은 필수**입니다.

### 로컬 실행 방법

#### 1. Gradle을 사용한 실행

```bash
# 개발 환경으로 실행
./gradlew bootRun --args='--spring.profiles.active=dev'

# 또는 JVM 옵션으로 프로파일 지정
./gradlew bootRun -Dspring.profiles.active=dev
```

#### 2. JAR 빌드 및 실행

```bash
# JAR 빌드
./gradlew bootJar

# 빌드된 JAR 실행 (개발 환경)
java -Dspring.profiles.active=dev -jar build/libs/AI-HUB-BE-0.0.1-SNAPSHOT.jar

# 빌드된 JAR 실행 (운영 환경)
java -Dspring.profiles.active=prod -jar build/libs/AI-HUB-BE-0.0.1-SNAPSHOT.jar
```

#### 3. 테스트 실행

```bash
./gradlew test
```

### Docker 실행 방법

#### Docker 이미지 빌드

```bash
# OCI 표준 컨테이너 이미지 빌드
docker build -t ai-hub-be:latest .
```

#### Docker 컨테이너 실행

```bash
# 개발 환경 실행 (H2 사용)
docker run -d \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=dev \
  -e KAKAO_CLIENT_ID=your_kakao_client_id \
  -e KAKAO_CLIENT_SECRET=your_kakao_client_secret \
  --name ai-hub-be \
  ai-hub-be:latest

# 운영 환경 실행 (PostgreSQL 사용)
docker run -d \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_URL=jdbc:postgresql://db-host:5432/aihub \
  -e DB_USERNAME=your_db_username \
  -e DB_PASSWORD=your_db_password \
  -e KAKAO_CLIENT_ID=your_kakao_client_id \
  -e KAKAO_CLIENT_SECRET=your_kakao_client_secret \
  -e JWT_SECRET=your_jwt_secret_key \
  -e DEPLOYMENT_ADDRESS=https://api.yourdomain.com \
  -e FRONTEND_ADDRESS=https://yourdomain.com \
  -e CORS_ALLOWED_ORIGINS=https://yourdomain.com \
  -e AI_SERVER_URL=http://ai-server:3000 \
  --name ai-hub-be \
  ai-hub-be:latest
```

#### Health Check

```bash
# 헬스 체크 확인
curl http://localhost:8080/actuator/health
```

## 프로젝트 구조

```
.
├── spec/                            # 설계/운영 문서
├── src/
│   ├── main/
│   │   ├── java/kr/ai_hub/AI_HUB_BE/
│   │   │   ├── AiHubBeApplication.java
│   │   │   ├── controller/          # REST API 엔드포인트
│   │   │   │   ├── admin/           # 관리자 AI 모델 API
│   │   │   │   ├── aimodel/         # 모델 조회 API
│   │   │   │   ├── auth/            # 로그인/토큰 API
│   │   │   │   ├── chat/            # 채팅 메시지/방 API
│   │   │   │   ├── dashboard/       # 대시보드 통계 API
│   │   │   │   ├── payment/         # 결제·코인 거래 API
│   │   │   │   ├── user/            # 사용자 정보 API
│   │   │   │   └── userwallet/      # 지갑 API
│   │   │   ├── application/         # 비즈니스 로직 (Service + DTO)
│   │   │   │   ├── admin/           # 관리자 모델 서비스
│   │   │   │   ├── aimodel/         # 모델 조회 서비스
│   │   │   │   ├── auth/            # OAuth2/JWT 서비스
│   │   │   │   │   ├── accesstoken/
│   │   │   │   │   └── refreshtoken/
│   │   │   │   ├── chat/            # 채팅방·메시지 서비스
│   │   │   │   │   ├── chatroom/
│   │   │   │   │   └── message/
│   │   │   │   ├── dashboard/       # 대시보드 통계 서비스
│   │   │   │   ├── payment/         # 결제·코인 서비스
│   │   │   │   ├── user/            # 사용자 서비스
│   │   │   │   └── userwallet/      # 지갑 서비스
│   │   │   ├── domain/              # 도메인 엔티티/리포지토리
│   │   │   │   ├── aimodel/
│   │   │   │   ├── auth/
│   │   │   │   ├── chat/
│   │   │   │   ├── payment/
│   │   │   │   ├── user/
│   │   │   │   └── userwallet/
│   │   │   └── global/              # 전역 설정/공통 컴포넌트
│   │   │       ├── application/     # 공통 서비스 (쿠키 등)
│   │   │       ├── auth/            # JWT, OAuth2, 필터/핸들러
│   │   │       ├── common/response/ # 공통 응답 DTO
│   │   │       ├── config/          # Security, OpenAPI, WebClient
│   │   │       └── error/           # 전역 예외 및 커스텀 예외
│   ├── resources/
│   │   ├── application.yaml
│   │   ├── application-dev.yaml
│   │   └── application-prod.yaml
│   └── test/
│       ├── java/kr/ai_hub/AI_HUB_BE/ # 통합/단위 테스트
│       └── resources/                # 테스트용 프로퍼티
└── gradle*, Dockerfile, build.gradle, settings.gradle, README.md 등 프로젝트 루트 자원
```

### 아키텍처 패턴

**Layered Architecture** (계층형 아키텍처)
```
Controller (REST API) → Application (Service) → Domain (Repository/Entity)
```

- **Controller**: HTTP 요청 처리, 입력 검증, 응답 변환
- **Application (Service)**: 비즈니스 로직, 트랜잭션 관리
- **Domain**: 엔티티, 리포지토리 (데이터베이스 접근)

**Package by Feature** (도메인별 패키지 구조)
- 각 도메인(user, chatroom, message 등)별로 패키지를 분리하여 응집도 향상

## API 문서

### Swagger UI

개발 환경에서 Swagger UI를 통해 API를 테스트할 수 있습니다:

```
http://localhost:8080/swagger-ui.html
```

### 주요 엔드포인트

#### 인증 (Authentication)
- `GET /api/v1/auth/login/oauth2` - OAuth2 로그인 (카카오)
- `POST /api/v1/auth/token/refresh` - Access Token 갱신
- `POST /api/v1/auth/logout` - 로그아웃

#### 사용자 (User)
- `GET /api/v1/users/me` - 현재 사용자 정보 조회
- `PATCH /api/v1/users/me` - 사용자 정보 수정

#### 채팅방 (ChatRoom)
- `GET /api/v1/chatrooms` - 채팅방 목록 조회
- `POST /api/v1/chatrooms` - 채팅방 생성
- `GET /api/v1/chatrooms/{id}` - 채팅방 상세 조회
- `PATCH /api/v1/chatrooms/{id}` - 채팅방 수정
- `DELETE /api/v1/chatrooms/{id}` - 채팅방 삭제

#### 메시지 (Message)
- `GET /api/v1/chatrooms/{roomId}/messages` - 메시지 목록 조회
- `POST /api/v1/chatrooms/{roomId}/messages` - 메시지 전송 (SSE 스트리밍 응답)
- `POST /api/v1/chatrooms/{roomId}/messages/upload` - 파일 업로드

#### 지갑 (Wallet)
- `GET /api/v1/wallet` - 지갑 정보 조회
- `GET /api/v1/wallet/balance` - 코인 잔액 조회

#### 대시보드 (Dashboard)
- `GET /api/v1/dashboard/usage/monthly` - 월별 사용량 조회
- `GET /api/v1/dashboard/stats/user` - 사용자 통계 조회
- `GET /api/v1/dashboard/models/pricing` - 모델 가격 정보 조회

#### 관리자 (Admin)
- `POST /api/v1/admin/ai-models` - AI 모델 등록
- `PATCH /api/v1/admin/ai-models/{id}` - AI 모델 수정
- `DELETE /api/v1/admin/ai-models/{id}` - AI 모델 삭제

### 인증 방식

모든 보호된 엔드포인트는 다음 두 가지 방식 중 하나로 인증합니다:

#### 1. Authorization 헤더 (권장)
```http
Authorization: Bearer <ACCESS_TOKEN>
```

#### 2. 쿠키 기반 인증
```http
Cookie: accessToken=<ACCESS_TOKEN>
```

자세한 API 명세는 [`spec/api.md`](spec/api.md)를 참조하세요.

## 배포

### CI/CD 파이프라인

GitHub Actions를 사용한 자동화된 CI/CD 파이프라인:

1. **테스트 실행**: JDK 25 환경에서 Gradle 테스트 실행
2. **컨테이너 이미지 빌드**: OCI 표준 컨테이너 이미지 빌드
3. **GHCR 푸시**: GitHub Container Registry에 이미지 푸시
4. **매니페스트 업데이트**: ArgoCD 매니페스트 리포지토리 업데이트 (이미지 태그 변경)

#### 트리거
- `main` 브랜치에 push 또는 PR 시 자동 실행
- `feat/cicd` 브랜치에 push 시 자동 실행

#### 이미지 레지스트리
- **Registry**: `ghcr.io`
- **Image**: `ghcr.io/<OWNER>/main-server:<TAG>`
- **Tags**:
  - `main` (브랜치명)
  - `main-<SHORT_SHA>` (브랜치-커밋SHA)
  - `<FULL_SHA>` (풀커밋 SHA, Kubernetes values.yaml과 일치)
  - `latest` (main 브랜치만)

### Kubernetes 배포

Kubernetes 클러스터에 배포하려면:

1. **매니페스트 리포지토리**: 별도 리포지토리에서 Helm Chart 관리
2. **ArgoCD**: GitOps 기반 자동 배포
3. **이미지 태그**: CI/CD 파이프라인이 자동으로 `values.yaml`의 이미지 태그 업데이트

자세한 배포 설정은 [`spec/CICD_SETUP.md`](spec/CICD_SETUP.md)를 참조하세요.

## 라이선스

이 프로젝트는 AI Hub 서비스의 백엔드 리포지토리입니다.
