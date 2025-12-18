# AI-HUB-BE 온보딩 가이드 (Backend)

> 마지막 업데이트: 2025-12-18  
> 이 문서는 **현재 AI-HUB-BE 코드**, **GitHub Actions CI/CD**, 그리고 `spec/` 문서들을 바탕으로 “처음 합류한 사람이 로컬에서 실행하고, 구조를 이해하고, 배포 흐름을 파악”하는 것을 목표로 합니다.

---

## 0) 이 레포가 하는 일 (한 줄 요약)

AI Hub는 여러 AI API(OpenAI, Anthropic, Google AI 등)를 **코인 기반 선불제**로 통합 제공하는 플랫폼이며, 이 레포(`AI-HUB-BE`)는 그 **백엔드(Spring Boot)** 입니다.

---

## 1) 5분 Quick Start (로컬 실행)

### 1-1. 준비물

- JDK: **25** (Gradle toolchain도 25로 고정)
- (선택) AI 서버: 로컬에서 채팅 스트리밍을 테스트하려면 별도 **AI 서버가 필요**합니다. (`AI_SERVER_URL`, 기본 `http://localhost:3000`)
- (권장) IntelliJ IDEA / VS Code Java 확장

### 1-2. 필수 환경변수

애플리케이션이 부팅되려면 최소 아래 값이 필요합니다(특히 OAuth2).

```bash
export KAKAO_CLIENT_ID="your_kakao_client_id"
export KAKAO_CLIENT_SECRET="your_kakao_client_secret"
```

> 로컬에서 `.env`를 쓰고 싶다면(권장): `.env`는 `.gitignore`에 포함되어 있으므로 개인 환경에서만 관리하세요.  
> zsh/bash 예시: `set -a; source .env; set +a`

로컬 `.env` 예시(필요에 따라 추가):

```bash
KAKAO_CLIENT_ID=...
KAKAO_CLIENT_SECRET=...
AI_SERVER_URL=http://localhost:3000
FRONTEND_ADDRESS=http://localhost:3000
DEPLOYMENT_ADDRESS=http://localhost:8080
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173,http://localhost:8080
```

### 1-3. 실행

```bash
./gradlew bootRun -Dspring.profiles.active=dev
```

### 1-4. 확인

- Health: `GET http://localhost:8080/actuator/health`
- Swagger UI(개발 프로필): `http://localhost:8080/swagger-ui.html`
- H2 Console(개발 프로필): `http://localhost:8080/h2-console`

### 1-5. 자주 쓰는 명령어

```bash
./gradlew test
./gradlew bootJar
./gradlew generateOpenApiDocs
```

### 1-6. (옵션) Docker로 실행

```bash
docker build -t ai-hub-be:local .

docker run --rm -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=dev \
  -e KAKAO_CLIENT_ID=your_kakao_client_id \
  -e KAKAO_CLIENT_SECRET=your_kakao_client_secret \
  -e AI_SERVER_URL=http://host.docker.internal:3000 \
  ai-hub-be:local
```

> `host.docker.internal`은 Docker Desktop(Mac/Windows)에서 주로 동작합니다. 리눅스라면 호스트 IP로 바꿔주세요.

---

## 2) 로컬 개발에서 “로그인”을 어떻게 하나요?

이 프로젝트의 기본 인증 흐름은 **카카오 OAuth2 로그인 → JWT 발급 → 쿠키 저장**입니다.

- 로그인 시작: `GET /oauth2/authorization/kakao`
- 로그인 성공 시 동작(코드 기준):
  - `accessToken` / `refreshToken`을 **HttpOnly 쿠키로 발급**
  - 프론트엔드 URL로 리다이렉트(`deployment.frontend.redirect-url`)
  - 발급된 토큰은 DB에도 저장/추적(폐기/만료/사용처리 포함)

중요 포인트(코드 기준):
- 백엔드는 `Authorization: Bearer <token>` **또는** `accessToken` 쿠키로 인증합니다. (`JwtAuthenticationFilter`)
- `refreshToken` 쿠키는 기본적으로 `path=/api/v1/token/refresh` 로 설정됩니다. 즉, **리프레시 엔드포인트에서만** 자동 전송됩니다. (`CookieService`)

인증 없이 접근 가능한 경로(현재 `SecurityConfig` 기준):

- `/h2-console/**`, `/oauth2/**`, `/api/v1/token/refresh`
- `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html`
- `/actuator/**`, `/favicon.ico`
- 모든 `OPTIONS /**` (CORS preflight)

---

## 3) “채팅 SSE 스트리밍”을 로컬에서 테스트하는 순서

### 3-1. 선행 조건

- 로그인 완료(쿠키가 브라우저에 저장되어 있어야 함)
- AI 서버 실행(로컬 기본): `AI_SERVER_URL=http://localhost:3000`
- 지갑 잔액이 0이면 메시지 전송이 실패합니다(코인 차감 로직 존재).

개발 환경에서 빠르게 잔액을 채우는 방법(둘 중 하나):

- (권장) H2 Console에서 ROLE/잔액 수정
  - H2 Console 접속: `http://localhost:8080/h2-console`
  - JDBC URL: `jdbc:h2:mem:testdb` / User: `sa` / Password: (빈 값)
  - 예시 SQL:
    - 관리자 승격: `UPDATE "user" SET role = 'ROLE_ADMIN' WHERE user_id = 1;`
    - 잔액 직접 수정: `UPDATE user_wallet SET balance = 10000, total_purchased = 10000 WHERE user_id = 1;`
- (대안) 관리자 API로 잔액 설정(ROLE_ADMIN 필요)
  - `PATCH /api/v1/admin/wallet?userId=1&amount=10000`

### 3-2. 테스트 플로우(권장)

1. 모델 목록 조회: `GET /api/v1/models`
2. 채팅방 생성: `POST /api/v1/chat-rooms`
3. (선택) 파일 업로드: `POST /api/v1/messages/files/upload` (multipart)
4. 메시지 전송(SSE): `POST /api/v1/messages/send/{roomId}` (`produces: text/event-stream`)

SSE는 대략 아래 이벤트들이 옵니다(현재 코드 기준).
- `started`: 처리 시작 알림
- `response`: 텍스트 조각 스트리밍
- `usage`: 최종 사용량/응답 요약(마지막에 1회)

CLI로 SSE를 보고 싶다면(토큰을 헤더로 넣는 방식):

```bash
curl -N \
  -H "Accept: text/event-stream" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <ACCESS_TOKEN>" \
  -d '{"message":"hello","modelId":1,"files":[]}' \
  "http://localhost:8080/api/v1/messages/send/<ROOM_ID>"
```

---

## 4) 개발/운영 설정(Profiles) 한눈에 보기

설정 파일:
- `src/main/resources/application.yaml`: 공통
- `src/main/resources/application-dev.yaml`: 개발
- `src/main/resources/application-prod.yaml`: 운영

핵심 차이(현재 코드 기준):
- 개발(`dev`)
  - DB 기본값: H2 in-memory (PostgreSQL 모드)
  - Swagger UI: enabled
  - 로그 레벨: 상세(DEBUG)
- 운영(`prod`)
  - DB: PostgreSQL 필수(`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`)
  - Swagger UI: 기본 disabled(`SWAGGER_ENABLED=false`)
  - 로그 레벨: 최소화(WARN 중심)

---

## 5) 프로젝트 구조(어디에 무엇이 있나요?)

최상위 패키지: `src/main/java/kr/ai_hub/AI_HUB_BE`

- `controller/`: REST API 엔드포인트
- `application/`: 유스케이스/비즈니스 로직(서비스 + DTO)
- `domain/`: JPA 엔티티/리포지토리(도메인 모델)
- `global/`: 보안, 설정, 공통 응답/예외 등 횡단 관심사

자세한 트리/현황은 `spec/codebase-structure.md`를 참고하세요.

주요 API 그룹(컨트롤러 기준):
- Auth: `/oauth2/authorization/kakao`, `/api/v1/token/refresh`, `/api/v1/auth/logout`
- AI Model: `/api/v1/models`, (관리자) `/api/v1/admin/models`
- Chat: `/api/v1/chat-rooms`, `/api/v1/messages/*`
- Wallet/Payment: `/api/v1/wallet`, `/api/v1/transactions`, `/api/v1/payments`
- Dashboard: `/api/v1/dashboard/*`

---

## 6) 응답/에러 컨벤션(디버깅할 때 꼭 봐야 함)

현재 백엔드의 기본 응답 래퍼는 `ApiResponse<T>` 입니다.

- 성공:
  - `success=true`
  - `detail`: 실제 데이터(없으면 `null`)
  - `timestamp`: 서버 생성 시각
- 실패:
  - 대부분 `ApiResponse<ErrorResponse>` 형태
  - 단, `@PreAuthorize` 권한 실패(`AccessDeniedException`)는 **`ErrorResponse` 단독 반환**(코드 기준)

컨벤션/예시는 `spec/convention.md`를 기준으로 봐주세요.

---

## 7) DB/도메인 모델(대략의 지도)

대표 도메인(개념 단위):
- **User/Auth**: 카카오 로그인 사용자 + Access/Refresh 토큰 저장/폐기/로테이션
- **Wallet/Coin**: 사용자 지갑, 결제 내역, 코인 거래 내역
- **AI Model**: 활성 모델 목록/가격 정책(관리자 기능 포함)
- **Chat**: 채팅방/메시지 저장 + AI 서버 SSE 스트리밍 연동
- **Dashboard**: 사용량/통계 집계

테이블/필드 기준 문서는 `spec/db.md`를 참고하세요.

---

## 8) AI 서버 연동(꼭 알아야 하는 부분)

AI-HUB-BE는 대략 아래처럼 AI 서버를 호출합니다(현재 코드 기준).
- 파일 업로드: `POST {AI_SERVER_URL}/ai/upload?model={modelName}` (multipart `file`)
- 채팅 스트리밍: `POST {AI_SERVER_URL}/ai/chat` (SSE)
- AI 서버 이벤트 형식(예):
  - `{"type":"response","data":"..."}` / `{"type":"usage","data":{...}}`

---

## 9) CI/CD & 브랜치 전략(처음 PR 올리기 전에)

### 9-1. GitHub Actions 워크플로우

- `/.github/workflows/cicd.yaml`
  - PR(`main`, `dev`, `release/**`): 테스트만 실행
  - `main` push: 테스트 → GHCR 이미지 빌드/푸시 → 매니페스트 리포(`values.yaml`)의 `springApp.image`를 **커밋 SHA로 업데이트**
- `/.github/workflows/docs-deploy.yml`
  - `main`, `dev` push 시 `./gradlew generateOpenApiDocs` 실행 후 GitHub Pages 배포(`docs/`)

배포/Secrets 설정은 `spec/CICD_SETUP.md`를 그대로 따르면 됩니다.

로컬에서 OpenAPI 산출물 업데이트:

```bash
./gradlew generateOpenApiDocs
```

- 결과물: `docs/openapi.json` (뷰어: `docs/index.html`)

### 9-2. 브랜치 규칙(자동 체크)

PR을 만들 때 브랜치 규칙 위반 시 `branch-rule.yaml` 워크플로우가 실패합니다.

- `main` ← `release/*` 또는 `hotfix/*` 만 허용
- `release/*` ← `dev` 또는 `hotfix/*` 만 허용
- `dev` ← `feat/*`, `hotfix/*`, `release/*`, `main` 만 허용

---

## 10) 자주 겪는 이슈(트러블슈팅)

- **앱이 부팅 중 죽음**: `KAKAO_CLIENT_ID`, `KAKAO_CLIENT_SECRET` 미설정 가능성이 큼
- **로그인은 됐는데 API가 401/403**: `SecurityConfig`에서 허용된 경로 외에는 인증 필요(기본 `anyRequest().authenticated()`)
- **리프레시가 동작 안 함**: `refreshToken` 쿠키가 `path=/api/v1/token/refresh` 인지 확인
- **쿠키/CORS 문제**: `sdd/troubleshoot/cookie-cross-domain.md` 참고
- **SSE가 끊김/멈춤**: AI 서버 실행 여부, `AI_SERVER_URL`, 네트워크/프록시, 타임아웃(5분) 확인

---

## 11) “더 읽을 것” (추천 순서)

1. `spec/onboard.md` (현재 문서)
2. `spec/codebase-structure.md` (패키지/엔드포인트 맵)
3. `spec/convention.md` (응답/예외/코딩 컨벤션)
4. `spec/db.md` (DB 스키마)
5. `spec/msa-ai-server.md` (AI 서버 구조/연동)
6. `spec/CICD_SETUP.md` (배포/CI/CD)
7. `spec/api.md` (API 스펙 문서/참고용)
