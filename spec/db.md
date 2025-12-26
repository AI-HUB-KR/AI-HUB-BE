## 데이터베이스 구조

### 엔티티 개요

AI HUB 플랫폼은 다음 9개의 주요 엔티티로 구성됩니다:

#### 1. User (사용자)
- **테이블명**: `"user"` (예약어이므로 따옴표 처리)
- **주요 필드**:
  - `user_id` (INT, PK): 사용자 고유 ID
  - `username` (VARCHAR(50)): 사용자명
  - `role` (VARCHAR(20)): 사용자 역할 (UserRole: ROLE_USER, ROLE_ADMIN)
  - `email` (VARCHAR(255)): 이메일
  - `kakao_id` (VARCHAR(255)): 카카오 사용자 고유 ID (유니크)
  - `profile_image_url` (VARCHAR(500)): 프로필 이미지 URL
  - `is_activated` (BOOLEAN): 활성화 상태
  - `is_deleted` (BOOLEAN): 삭제 상태 (소프트 삭제)
  - `created_at` (TIMESTAMP): 생성 시간
  - `deleted_at` (TIMESTAMP): 삭제 시간
- **비고**:
  - 도메인 메서드: `updateRole(UserRole role)` - 사용자 권한 수정 (관리자 기능)

#### 2. ChatRoom (채팅방)
- **테이블명**: `chat_room`
- **주요 필드**:
  - `room_id` (UUID, PK): 채팅방 고유 ID (UUIDv7)
  - `user_id` (INT, FK): 사용자 ID
  - `title` (VARCHAR(30)): 채팅방 제목
  - `coin_usage` (DECIMAL(20,10)): 코인 사용량
  - `created_at`, `updated_at` (TIMESTAMP): 생성/수정 시간
- **비고**:
  - 도메인 메서드: `addCoinUsage(BigDecimal amount)` - 코인 사용량 누적
- **관계**: User와 N:1 관계

#### 3. Message (메시지)
- **테이블명**: `message`
- **주요 필드**:
  - `message_id` (UUID, PK): 메시지 고유 ID (UUIDv7)
  - `room_id` (UUID, FK): 채팅방 ID
  - `role` (VARCHAR(10)): 역할 (MessageRole Enum: USER, ASSISTANT)
  - `content` (TEXT): 메시지 내용
  - `file_url` (VARCHAR(500)): 첨부 파일 URL (AI 서버 파일 ID)
  - `token_count` (DECIMAL(20,10)): 토큰 수
  - `coin_count` (DECIMAL(20,10)): 코인 수
  - `model_id` (INT, FK): AI 모델 ID
  - `response_id` (VARCHAR(100)): AI 서버 응답 ID (대화 연결용)
  - `created_at` (TIMESTAMP): 생성 시간
- **비고**:
  - `role` 필드는 MessageRole Enum (USER, ASSISTANT)을 사용하며, DB에는 "USER", "ASSISTANT" 문자열로 저장됨 (EnumType.STRING)
  - `response_id`는 AI 서버가 생성한 응답 ID로, 대화 맥락 연결에 사용됨
  - 도메인 메서드: `updateTokenAndCoin()`, `updateResponseId()`
- **관계**:
  - ChatRoom과 N:1 관계
  - AIModel과 N:1 관계

#### 4. RefreshToken (리프레시 토큰)
- **테이블명**: `refresh_token`
- **주요 필드**:
  - `token_id` (INT, PK): 토큰 고유 ID
  - `user_id` (INT, FK): 사용자 ID
  - `token_hash` (VARCHAR(64)): 토큰 해시 (유니크)
  - `created_at` (TIMESTAMP): 생성 시간
  - `expires_at` (TIMESTAMP): 만료 시간
  - `is_revoked` (BOOLEAN): 폐기 여부
  - `revoked_at` (TIMESTAMP): 폐기 시간
  - `revoked_reason` (VARCHAR(100)): 폐기 사유
  - `last_used_at` (TIMESTAMP): 마지막 사용 시간
- **비고**: 원본 리프레시 토큰은 노출을 방지하기 위해 SHA-256으로 해시하여 저장하며,
  로그인/토큰 재발급 시 동일한 해시 값이 존재하고 폐기되지 않았는지 검증한다. 토큰 재발급 시 기존 토큰은
  `is_revoked=true`로 상태를 갱신하고 신규 토큰 해시를 저장한다. `revoked_reason` 필드는 `TokenRevokeReason`
  enum(`ROTATED`, `EXPIRED`, `USER_LOGOUT`) 값을 문자열로 보관한다.
- **관계**: User와 N:1 관계

#### 5. AccessToken (액세스 토큰)
- **테이블명**: `access_token`
- **주요 필드**:
  - `token_id` (INT, PK): 토큰 고유 ID
  - `user_id` (INT, FK): 사용자 ID
  - `refresh_token_id` (INT, FK, NULLABLE): 발급 근거가 된 리프레시 토큰 ID
  - `token_hash` (VARCHAR(64)): 토큰 해시 (유니크)
  - `issued_at` (TIMESTAMP): 발급 시간
  - `expires_at` (TIMESTAMP): 만료 시간
  - `last_used_at` (TIMESTAMP): 마지막 사용 시간
  - `is_revoked` (BOOLEAN): 폐기 여부
  - `revoked_at` (TIMESTAMP): 폐기 시간
  - `revoked_reason` (VARCHAR(100)): 폐기 사유
- **비고**: 원본 액세스 토큰은 외부 노출 방지를 위해 SHA-256으로 해시하여 저장하며, 만료되었거나 폐기된 토큰은 요청 시 즉시 거부한다.
  `revoked_reason` 필드는 `TokenRevokeReason` enum 값을 문자열로 보관하고, 리프레시 토큰 폐기와 연계해 일괄 업데이트된다.
- **관계**:
  - User와 N:1 관계
  - RefreshToken과 N:1 관계 (nullable)

#### 6. AIModel (AI 모델)
- **테이블명**: `ai_model`
- **주요 필드**:
  - `model_id` (INT, PK): 모델 고유 ID
  - `model_name` (VARCHAR(50)): 모델명 (유니크)
  - `display_name` (VARCHAR(100)): 표시명
  - `display_explain` (VARCHAR(100)): 모델 설명
  - `input_price_per_1m` (DECIMAL(20,10)): 입력 1m 토큰당 가격
  - `output_price_per_1m` (DECIMAL(20,10)): 출력 1m 토큰당 가격
  - `model_markup_rate` (DECIMAL(5,4)): 모델 마크업 비율 (예: 0.2 = 20%)
  - `is_active` (BOOLEAN): 활성화 상태
  - `created_at`, `updated_at` (TIMESTAMP): 생성/수정 시간

#### 7. UserWallet (사용자 지갑)
- **테이블명**: `user_wallet`
- **주요 필드**:
  - `wallet_id` (INT, PK): 지갑 고유 ID
  - `user_id` (INT, FK, UNIQUE): 사용자 ID (유니크)
  - `balance` (DECIMAL(20,10)): 총 잔액 (paid_balance + promotion_balance)
  - `paid_balance` (DECIMAL(20,10)): 유상 코인 잔액 (결제로 충전된 코인)
  - `promotion_balance` (DECIMAL(20,10)): 프로모션 코인 잔액 (관리자가 지급한 무상 코인)
  - `total_purchased` (DECIMAL(20,10)): 총 구매 금액
  - `total_used` (DECIMAL(20,10)): 총 사용 금액
  - `last_transaction_at` (TIMESTAMP): 마지막 거래 시간
  - `created_at`, `updated_at` (TIMESTAMP): 생성/수정 시간
- **비고**:
  - 도메인 메서드:
    - `addPaidBalance(BigDecimal amount)` - 유상 코인 증가 (결제 시)
    - `addPromotionBalance(BigDecimal amount)` - 프로모션 코인 증가 (관리자 지급)
    - `deductPromotionBalance(BigDecimal amount)` - 프로모션 코인 감소 (관리자 회수, 잔액 부족 시 예외)
    - `deductBalance(BigDecimal amount)` - 코인 사용 (프로모션 코인 선차감 후 유상 코인 차감)
  - 코인 사용 우선순위: 프로모션 코인 → 유상 코인 순으로 차감
  - 모든 잔액 변경 시 `last_transaction_at` 자동 업데이트
- **관계**: User와 1:1 관계

#### 8. WalletHistory (결제 내역 / 지갑 변동 이력)
- **테이블명**: `wallet_history`
- **용도**:
  - 결제를 통한 유상 코인 충전 이력 기록
  - 관리자의 프로모션 코인 지급/회수 이력 기록
  - 모든 지갑 잔액 변동 사항의 추적 가능한 이력 관리
- **주요 필드**:
  - `history_id` (BIGINT, PK): 결제 고유 ID
  - `user_id` (INT, FK): 사용자 ID
  - `transaction_id` (VARCHAR(100)): 거래 ID (유니크)
    - 결제: 결제 게이트웨이 제공 ID
    - 관리자 작업: `admin_promo_{UUID}` 형식
  - `payment_method` (VARCHAR(50)): 결제 수단
  - `pay_amount_krw` (DECIMAL(20,2)): 원화 결제 금액
  - `pay_amount_usd` (DECIMAL(20,2)): 달러 결제 금액
  - `paid_coin` (DECIMAL(20,10)): 유상 코인 금액
  - `promotion_coin` (DECIMAL(20,10)): 프로모션 코인 금액 (기본값: 0)
  - `status` (VARCHAR(20)): 처리 상태 (기본값: 'pending')
  - `wallet_history_type` (VARCHAR(20)): 지갑 이력 타입 (WalletHistoryType Enum)
    - `PAID`: 유상 코인 지급 (결제)
    - `PROMOTION`: 프로모션 코인 지급 (관리자)
    - `PROMOTION_RETRIEVE`: 프로모션 코인 회수 (관리자)
    - `PAID_RETRIEVE`: 유상 코인 환불 (관리자)
  - `payment_gateway` (VARCHAR(50)): 결제 게이트웨이
  - `metadata` (JSONB): 메타데이터
    - 관리자 작업 시 포함 정보: `adminId`, `userId`, `reason`
  - `created_at` (TIMESTAMP): 생성 시간
  - `completed_at` (TIMESTAMP): 완료 시간
- **비고**:
  - 도메인 메서드: `complete()`, `fail(String reason)`
  - 관리자의 모든 프로모션 코인 변경 작업은 이 테이블에 기록됨
  - `wallet_history_type`으로 이력 타입을 명확히 구분하여 조회 및 통계 생성 용이
- **관계**: User와 N:1 관계

#### 9. CoinTransaction (코인 거래 / 코인 사용 이력)
- **테이블명**: `coin_transaction`
- **용도**:
  - AI 메시지 생성에 따른 코인 사용 이력 기록
  - 사용자의 코인 소비 패턴 분석 및 통계 생성
- **주요 필드**:
  - `transaction_id` (BIGINT, PK): 거래 고유 ID
  - `user_id` (INT, FK): 사용자 ID
  - `room_id` (UUID, FK): 채팅방 ID (nullable)
  - `message_id` (UUID, FK): 메시지 ID (nullable)
  - `transaction_type` (VARCHAR(20)): 거래 유형 (예: "message_usage")
  - `coin_usage` (DECIMAL(20,10)): 사용된 코인 금액
  - `balance_after` (DECIMAL(20,10)): 거래 후 잔액 (스냅샷 - 최신 잔액 아님)
  - `description` (TEXT): 거래 설명
  - `model_id` (INT, FK): 사용된 AI 모델 ID (nullable)
  - `created_at` (TIMESTAMP): 생성 시간
- **비고**:
  - `balance_after`는 해당 거래 당시의 잔액 스냅샷으로, 현재 최신 잔액을 나타내지 않음
  - 메시지 전송 시 코인 차감과 동시에 이력이 기록됨
  - WalletHistory와 달리 코인 사용(차감) 이력만 기록
- **관계**:
  - User와 N:1 관계
  - ChatRoom과 N:1 관계 (nullable)
  - Message와 N:1 관계 (nullable)
  - AIModel과 N:1 관계 (nullable)

### Enum 타입 정의

#### WalletHistoryType (지갑 이력 타입)
지갑 잔액 변동 이력의 타입을 구분하는 Enum입니다. WalletHistory 테이블의 `wallet_history_type` 필드에서 사용됩니다.

- **PAID**: 유상 코인 지급 (결제를 통한 코인 충전)
- **PROMOTION**: 프로모션 코인 지급 (관리자가 사용자에게 무상 지급)
- **PROMOTION_RETRIEVE**: 프로모션 코인 회수 (관리자가 사용자로부터 회수)
- **PAID_RETRIEVE**: 유상 코인 환불 (관리자가 유상 코인 회수 - 환불 처리)

**활용 방법**:
- 결제 시스템: `PAID` 타입으로 유상 코인 충전 이력 생성
- 관리자 프로모션: `PROMOTION` 타입으로 무상 코인 지급 이력 생성
- 관리자 회수: `PROMOTION_RETRIEVE` 또는 `PAID_RETRIEVE` 타입으로 회수 이력 생성
- 통계 및 조회: 타입별로 이력을 필터링하여 충전/지급/회수 현황 파악

### 데이터베이스 설계 특징

- **UUID 사용**: `chat_room`, `message` 엔티티는 UUIDv7을 PK로 사용
- **Soft Delete**: User 엔티티는 `is_deleted`, `deleted_at` 필드를 통한 소프트 삭제 지원
- **정밀한 금액 처리**: 코인 관련 필드는 DECIMAL(20,10)로 정밀한 계산 지원
- **이중 잔액 관리**: UserWallet은 `paid_balance`(유상 코인)와 `promotion_balance`(프로모션 코인)를 분리 관리
- **코인 사용 우선순위**: 프로모션 코인을 먼저 차감한 후 유상 코인 차감 (UserWallet.deductBalance 메서드)
- **이력 추적**:
  - WalletHistory: 코인 충전/지급/회수 이력 (잔액 증가/감소 모두 포함)
  - CoinTransaction: 코인 사용(차감) 이력만 기록
- **연관 관계/삭제 정책**:
  - User ↔ UserWallet: 1:1 (JPA: `cascade = ALL`, `orphanRemoval = true`)
  - 그 외 관계는 기본적으로 cascade 설정 없음 (DB의 ON DELETE 동작은 스키마/마이그레이션에 따름)
- **JSONB 지원**: WalletHistory의 metadata 필드는 유연한 데이터 저장을 위해 JSONB 타입 사용
