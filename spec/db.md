## 데이터베이스 구조

### 엔티티 개요

AI HUB 플랫폼은 다음 9개의 주요 엔티티로 구성됩니다:

#### 1. User (사용자)
- **테이블명**: `user`
- **주요 필드**:
  - `user_id` (INT, PK): 사용자 고유 ID
  - `username` (VARCHAR(50)): 사용자명
  - `role` (VARCHAR(10)): 사용자 역할 (예: admin, user)
  - `email` (VARCHAR(255)): 이메일
  - `kakao_id` (VARCHAR(255)): 카카오 사용자 고유 ID (유니크)
  - `profile_image_url` (VARCHAR(500)): 프로필 이미지 URL
  - `is_activated` (BOOLEAN): 활성화 상태
  - `is_deleted` (BOOLEAN): 삭제 상태 (소프트 삭제)
  - `created_at` (TIMESTAMP): 생성 시간
  - `deleted_at` (TIMESTAMP): 삭제 시간

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
- **관계**: User와 N:1 관계 (cascade delete)

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
  - `role` 필드는 MessageRole Enum (USER, ASSISTANT)을 사용하며, DB에는 "user", "assistant" 문자열로 저장됨
  - `response_id`는 AI 서버가 생성한 응답 ID로, 대화 맥락 연결에 사용됨
  - 도메인 메서드: `updateTokenAndCoin()`, `updateResponseId()`
- **관계**:
  - ChatRoom과 N:1 관계 (cascade delete)
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
- **비고**: 원본 리프레시 토큰은 노출을 방지하기 위해 SHA-256으로 해시하여 저장하며,
  로그인/토큰 재발급 시 동일한 해시 값이 존재하고 폐기되지 않았는지 검증한다. 토큰 재발급 시 기존 토큰은
  `is_revoked=true`로 상태를 갱신하고 신규 토큰 해시를 저장한다. `revoked_reason` 필드는 `TokenRevokeReason`
  enum(`ROTATED`, `EXPIRED`, `USER_LOGOUT`) 값을 문자열로 보관한다.
- **관계**: User와 N:1 관계 (cascade delete)

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
  - User와 N:1 관계 (cascade delete)
  - RefreshToken과 N:1 관계 (set null on delete)

#### 6. AIModel (AI 모델)
- **테이블명**: `ai_model`
- **주요 필드**:
  - `model_id` (INT, PK): 모델 고유 ID
  - `model_name` (VARCHAR(50)): 모델명 (유니크)
  - `display_name` (VARCHAR(100)): 표시명
  - `display_explain` (VARCHAR(100)): 모델 설명
  - `input_price_per_1m` (DECIMAL(20,10)): 입력 1m 토큰당 가격
  - `output_price_per_1m` (DECIMAL(20,10)): 출력 1m 토큰당 가격
  - `is_active` (BOOLEAN): 활성화 상태
  - `created_at`, `updated_at` (TIMESTAMP): 생성/수정 시간

#### 7. UserWallet (사용자 지갑)
- **테이블명**: `user_wallet`
- **주요 필드**:
  - `wallet_id` (INT, PK): 지갑 고유 ID
  - `user_id` (INT, FK, UNIQUE): 사용자 ID (유니크)
  - `balance` (DECIMAL(20,10)): 잔액
  - `total_purchased` (DECIMAL(20,10)): 총 구매 금액
  - `total_used` (DECIMAL(20,10)): 총 사용 금액
  - `last_transaction_at` (TIMESTAMP): 마지막 거래 시간
  - `created_at`, `updated_at` (TIMESTAMP): 생성/수정 시간
- **관계**: User와 1:1 관계 (cascade delete)

#### 8. PaymentHistory (결제 내역)
- **테이블명**: `payment_history`
- **주요 필드**:
  - `payment_id` (BIGINT, PK): 결제 고유 ID
  - `user_id` (INT, FK): 사용자 ID
  - `transaction_id` (VARCHAR(100)): 거래 ID (유니크)
  - `payment_method` (VARCHAR(50)): 결제 수단
  - `amount_krw` (DECIMAL(20,2)): 원화 금액
  - `amount_usd` (DECIMAL(20,2)): 달러 금액
  - `coin_amount` (DECIMAL(20,10)): 코인 금액
  - `bonus_coin` (DECIMAL(20,10)): 보너스 코인
  - `status` (VARCHAR(20)): 결제 상태 (기본값: 'pending')
  - `payment_gateway` (VARCHAR(50)): 결제 게이트웨이
  - `metadata` (JSONB): 메타데이터
  - `created_at`, `completed_at` (TIMESTAMP): 생성/완료 시간
- **관계**: User와 N:1 관계 (cascade delete)

#### 9. CoinTransaction (코인 거래)
- **테이블명**: `coin_transaction`
- **주요 필드**:
  - `transaction_id` (BIGINT, PK): 거래 고유 ID
  - `user_id` (INT, FK): 사용자 ID
  - `room_id` (UUID, FK): 채팅방 ID (선택)
  - `message_id` (UUID, FK): 메시지 ID (선택)
  - `transaction_type` (VARCHAR(20)): 거래 유형
  - `amount` (DECIMAL(20,10)): 거래 금액
  - `balance_after` (DECIMAL(20,10)): 거래 후 잔액
  - `description` (TEXT): 설명
  - `model_id` (INT, FK): AI 모델 ID
  - `created_at` (TIMESTAMP): 생성 시간
- **관계**:
  - User와 N:1 관계 (cascade delete)
  - ChatRoom과 N:1 관계 (set null on delete)
  - Message와 N:1 관계 (set null on delete)
  - AIModel과 N:1 관계 (set null on delete)

### 데이터베이스 설계 특징

- **UUID 사용**: `chat_room`, `message` 엔티티는 UUIDv7을 PK로 사용
- **Soft Delete**: User 엔티티는 `is_deleted`, `deleted_at` 필드를 통한 소프트 삭제 지원
- **정밀한 금액 처리**: 코인 관련 필드는 DECIMAL(20,10)로 정밀한 계산 지원
- **CASCADE 전략**:
  - User 삭제 시 관련 데이터 자동 삭제 (chat_room, refresh_token, access_token, user_wallet, payment_history, coin_transaction)
  - ChatRoom 삭제 시 메시지 자동 삭제
  - 일부 FK는 SET NULL 전략 사용 (coin_transaction의 room_id, message_id, model_id)
- **JSONB 지원**: PaymentHistory의 metadata 필드는 유연한 데이터 저장을 위해 JSONB 타입 사용
