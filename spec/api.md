## API 응답 구조 표준
> swagger와 코드를 통해 최신 내용을 확인하는 것을 **강력히 추천** 합니다.
> 해당 문서는 **참고용** 으로 사용하시는 것을 권장합니다.

대부분의 REST API는 일관된 `ApiResponse<T>` 형태로 응답합니다.

단, 아래 케이스는 예외입니다.
- `204 No Content` 응답: 본문 없음
- Spring Security의 `AccessDeniedException`(예: 관리자 권한 부족) 응답: `ErrorResponse` 단독 객체로 반환될 수 있음
- SSE 스트리밍 응답: `text/event-stream` (일반 JSON 래핑 아님)

---

### 성공 응답 구조
```json
{
  "success": true,
  "detail": {
    "any": "data"
  },
  "timestamp": "2025-01-01T00:00:00Z"
}
```

**필드**
- `success`: 성공 여부 (`true`)
- `detail`: 실제 응답 데이터 (없으면 `null`)
- `timestamp`: 응답 생성 시각 (`Instant`, ISO 8601)

### 실패 응답 구조
```json
{
  "success": false,
  "detail": {
    "code": "ERROR_CODE",
    "message": "오류 메시지",
    "details": "상세 메시지(선택)"
  },
  "timestamp": "2025-01-01T00:00:00Z"
}
```

**필드**
- `detail.code`: 에러 코드 (주로 `ErrorCode` enum 이름)
- `detail.message`: 고정 메시지 (`ErrorCode.message`)
- `detail.details`: 예외 상세 메시지 (선택, `null`이면 JSON에서 생략될 수 있음)

---

### HTTP Status Code 매핑 (GlobalExceptionHandler 기준)

| HTTP Status | code | 비고 |
|---|---|---|
| 200 | - | 성공 |
| 201 | - | 생성 성공 |
| 204 | - | 성공, 본문 없음 |
| 400 | `VALIDATION_ERROR` 등 | 검증/비즈니스 오류 기본값 |
| 401 | `AUTHENTICATION_FAILED`, `INVALID_TOKEN` | 인증 실패/토큰 문제 |
| 403 | `FORBIDDEN` | 권한 없음 |
| 404 | `*_NOT_FOUND` | 리소스 없음 |
| 409 | `SYSTEM_ILLEGAL_STATE` | 시스템 상태 충돌 |
| 502 | `AI_SERVER_ERROR` | AI 서버 통신 실패 |
| 503 | `SERVICE_UNAVAILABLE` | 서비스 이용 불가 |
| 500 | `INTERNAL_SERVER_ERROR` | 서버 내부 오류 |

---

### 인증 방식

#### 1) Authorization 헤더
```http
Authorization: Bearer <ACCESS_TOKEN>
```

#### 2) 쿠키 기반 (HttpOnly)
쿠키 이름은 고정입니다.
- `accessToken`: Path=`/`
- `refreshToken`: Path=`/api/v1/token/refresh` (기본값, 환경설정으로 변경 가능)

쿠키 속성은 환경설정(`application-*.yaml`)에 따라 달라집니다.
- `cookie.secure`: dev 기본 `false`, prod 기본 `true`
- `cookie.same-site`: dev/prod 기본 `Lax`
- `cookie.domain`: dev 기본 `localhost`, prod `aihub.io.kr`

브라우저에서 쿠키 인증을 쓰려면 요청에 `credentials: "include"`가 필요합니다.

---

### OAuth2 로그인 (카카오)

#### 카카오 소셜 로그인 시작
- **Method**: GET `/oauth2/authorization/kakao`
- **설명**: 카카오 로그인 페이지로 리다이렉트합니다.
- **성공**: `302 Found`

#### 로그인 성공 후 동작
- 서버가 `accessToken`, `refreshToken` 쿠키를 설정
- `deployment.frontend.redirect-url`로 리다이렉트

---

### 토큰 갱신/로그아웃

#### 토큰 갱신
- **Method**: POST `/api/v1/token/refresh`
- **설명**: `refreshToken` 쿠키로 토큰을 회전 발급하고, 새 쿠키를 응답에 설정합니다.
- **인증**: Public (단, `refreshToken` 쿠키 필요)

**요청**
```http
POST /api/v1/token/refresh
Cookie: refreshToken=<REFRESH_TOKEN>
```

**성공 응답**
- **200 OK** (본문은 `detail: null`)
```json
{
  "success": true,
  "detail": null,
  "timestamp": "2025-01-01T00:00:00Z"
}
```

**응답 헤더 예시**
```http
Set-Cookie: accessToken=<NEW_ACCESS_TOKEN>; HttpOnly; Path=/; Max-Age=...
Set-Cookie: refreshToken=<NEW_REFRESH_TOKEN>; HttpOnly; Path=/api/v1/token/refresh; Max-Age=...
```

**오류 응답 예시**
- **401 Unauthorized** (`INVALID_TOKEN`): 리프레시 토큰이 없거나 유효하지 않음
```json
{
  "success": false,
  "detail": {
    "code": "INVALID_TOKEN",
    "message": "유효하지 않은 토큰입니다",
    "details": "Refresh token cookie is missing"
  },
  "timestamp": "2025-01-01T00:00:00Z"
}
```

#### 로그아웃
- **Method**: POST `/api/v1/auth/logout`
- **설명**: 사용자의 Refresh/Access 토큰을 폐기하고 쿠키를 제거합니다.
- **인증**: 필수 (Access Token)

**요청**
```http
POST /api/v1/auth/logout
Authorization: Bearer <ACCESS_TOKEN>
```

**성공 응답**
- **204 No Content**

---

### 에러 코드 (ErrorCode)

#### 인증 전/공통
| 코드 | 설명 |
|---|---|
| `AUTHENTICATION_FAILED` | 인증에 실패했습니다 |
| `VALIDATION_ERROR` | 입력값 검증에 실패했습니다 |
| `UNSUPPORTED_OAUTH2_PROVIDER` | 지원하지 않는 OAuth2 공급자입니다 |

#### 인증 후
| 코드 | 설명 |
|---|---|
| `USER_NOT_FOUND` | 사용자를 찾을 수 없습니다 |
| `FORBIDDEN` | 접근 권한이 없습니다 |
| `INVALID_TOKEN` | 유효하지 않은 토큰입니다 |
| `INSUFFICIENT_BALANCE` | 코인 잔액이 부족합니다 |
| `WALLET_NOT_FOUND` | 지갑을 찾을 수 없습니다 |
| `ROOM_NOT_FOUND` | 채팅방을 찾을 수 없습니다 |
| `MESSAGE_NOT_FOUND` | 메시지를 찾을 수 없습니다 |
| `MODEL_NOT_FOUND` | AI 모델을 찾을 수 없습니다 |
| `WALLET_HISTORY_NOT_FOUND` | 지갑 이력을 찾을 수 없습니다 |
| `PAYMENT_FAILED` | 결제에 실패했습니다 |
| `TRANSACTION_NOT_FOUND` | 거래 내역을 찾을 수 없습니다 |
| `SYSTEM_ILLEGAL_STATE` | 시스템 상태가 유효하지 않습니다 |
| `AI_SERVER_ERROR` | AI 서버와의 통신에 실패했습니다 |
| `INTERNAL_SERVER_ERROR` | 서버 내부 오류가 발생했습니다 |
| `SERVICE_UNAVAILABLE` | 서비스를 사용할 수 없습니다 |

#### 기타(안전망)
아래 코드는 `ErrorCode` enum이 아닌 예외 처리 안전망에서만 사용될 수 있습니다.
| 코드 | 설명 |
|---|---|
| `RESPONSE_STATUS_EXCEPTION` | `ResponseStatusException` 처리 |
| `UNKNOWN_ERROR` | 알 수 없는 오류(직접 생성 시) |

---

### 토큰 폐기 사유 (TokenRevokeReason)
| 사유 | 설명 |
|---|---|
| `ROTATED` | 토큰 갱신으로 인한 폐기 |
| `EXPIRED` | 만료로 인한 폐기 |
| `USER_LOGOUT` | 로그아웃으로 인한 폐기 |

---
---

## API 엔드포인트

### 1. 사용자 (User)

#### 내 정보 조회
- **Method**: GET `/api/v1/users/me`
- **인증**: 필수

**성공 응답 (200)**
```json
{
  "success": true,
  "detail": {
    "userId": 1,
    "username": "string",
    "email": "string",
    "isActivated": true,
    "createdAt": "2025-01-01T00:00:00Z"
  },
  "timestamp": "2025-01-01T00:00:00Z"
}
```

#### 내 정보 수정
- **Method**: PUT `/api/v1/users/me`
- **인증**: 필수

**요청 본문**
```json
{
  "username": "string",
  "email": "string"
}
```

**요청 제약**
| 필드 | 제약 |
|---|---|
| `username` | 2~30자, 공백 불가 |
| `email` | 이메일 형식 |

**성공 응답 (200)**: `내 정보 조회`와 동일

**오류 예시**
- **400** `VALIDATION_ERROR`: 이메일/사용자명 중복 또는 검증 실패

#### 회원 탈퇴
- **Method**: DELETE `/api/v1/users/me`
- **인증**: 필수
- **성공**: `204 No Content`

---

### 2. 채팅방 (ChatRoom)

#### 채팅방 생성
- **Method**: POST `/api/v1/chat-rooms`
- **인증**: 필수

**요청 본문**
```json
{
  "title": "새로운 채팅방",
  "modelId": 1
}
```

**요청 제약**
| 필드 | 제약 |
|---|---|
| `title` | 공백 불가, 최대 30자 |
| `modelId` | 필수 |

**성공 응답 (201)**
```json
{
  "success": true,
  "detail": {
    "roomId": "uuid",
    "title": "새로운 채팅방",
    "userId": 1,
    "coinUsage": 0.0,
    "createdAt": "2025-01-01T00:00:00Z",
    "updatedAt": "2025-01-01T00:00:00Z"
  },
  "timestamp": "2025-01-01T00:00:00Z"
}
```

**응답 필드 (ChatRoomResponse)**
| 필드 | 타입 | 설명 |
|---|---|---|
| `detail.roomId` | string | 채팅방 UUID |
| `detail.title` | string | 채팅방 제목 |
| `detail.userId` | integer | 채팅방 소유자 ID |
| `detail.coinUsage` | number | 누적 코인 사용량 |
| `detail.createdAt` | string | 생성 시각 (ISO 8601) |
| `detail.updatedAt` | string | 수정 시각 (ISO 8601) |

**오류 예시**
- **404** `MODEL_NOT_FOUND`: 존재하지 않거나 비활성화된 모델

#### 채팅방 목록 조회
- **Method**: GET `/api/v1/chat-rooms`
- **인증**: 필수

**쿼리 파라미터**
| 파라미터 | 기본값 | 설명 |
|---|---:|---|
| `page` | 0 | 0부터 시작 |
| `size` | 20 | 페이지 크기 |
| `sort` | `createdAt,desc` | `필드,asc|desc` |

**성공 응답 (200)**: `detail`은 Spring Data `Page` 직렬화 형태입니다. (`content`, `totalElements`, `totalPages`, `number`, `size` 외에 `pageable/sort/first/last/empty` 등이 포함될 수 있음)

**content[] 필드 (ChatRoomListItemResponse)**
| 필드 | 타입 | 설명 |
|---|---|---|
| `detail.content[].roomId` | string | 채팅방 UUID |
| `detail.content[].title` | string | 채팅방 제목 |
| `detail.content[].coinUsage` | number | 누적 코인 사용량 |
| `detail.content[].lastMessageAt` | string | 마지막 메시지 시각 (없으면 `null`) |
| `detail.content[].createdAt` | string | 생성 시각 (ISO 8601) |

#### 채팅방 상세 조회
- **Method**: GET `/api/v1/chat-rooms/{roomId}`
- **인증**: 필수

**성공 응답 (200)**: `채팅방 생성`의 `ChatRoomResponse`와 동일

**오류 예시**
- **404** `ROOM_NOT_FOUND`
- **403** `FORBIDDEN`

#### 채팅방 제목 수정
- **Method**: PUT `/api/v1/chat-rooms/{roomId}`
- **인증**: 필수

**요청 본문**
```json
{
  "title": "수정된 채팅방 제목"
}
```

**성공 응답 (200)**: `채팅방 생성`의 `ChatRoomResponse`와 동일

#### 채팅방 삭제
- **Method**: DELETE `/api/v1/chat-rooms/{roomId}`
- **인증**: 필수
- **성공**: `204 No Content`

---

### 3. 메시지 (Message)

#### 파일 업로드
- **Method**: POST `/api/v1/messages/files/upload`
- **인증**: 필수
- **Consumes**: `multipart/form-data`

**요청 (multipart/form-data)**
| 필드 | 위치 | 필수 | 설명 |
|---|---|---:|---|
| `file` | part | ✅ | 이미지 파일 (`jpg/jpeg/png/webp`, 최대 10MB) |
| `modelId` | param | ✅ | AI 모델 ID |

**성공 응답 (201)**
```json
{
  "success": true,
  "detail": {
    "fileId": "file-abc123"
  },
  "timestamp": "2025-01-01T00:00:00Z"
}
```

**응답 필드 (FileUploadResponse)**
| 필드 | 타입 | 설명 |
|---|---|---|
| `detail.fileId` | string | AI 서버가 발급한 파일 ID |

**오류 예시**
- **400** `VALIDATION_ERROR`: 파일 검증 실패
- **404** `MODEL_NOT_FOUND`
- **502** `AI_SERVER_ERROR`: AI 서버 업로드 실패

#### 메시지 목록 조회
- **Method**: GET `/api/v1/messages/page/{roomId}`
- **인증**: 필수

**쿼리 파라미터**
| 파라미터 | 기본값 | 설명 |
|---|---:|---|
| `page` | 0 | 0부터 시작 |
| `size` | 50 | 페이지 크기 |
| `sort` | `createdAt,asc` | `필드,asc|desc` |

**성공 응답 (200)**: `detail`은 Spring Data `Page` 직렬화 형태입니다.

**content[] 필드 (MessageListItemResponse)**
| 필드 | 타입 | 설명 |
|---|---|---|
| `detail.content[].messageId` | string | 메시지 UUID |
| `detail.content[].role` | string | 역할 (`user`, `assistant`) |
| `detail.content[].content` | string | 메시지 내용 |
| `detail.content[].tokenCount` | number | 토큰 수 |
| `detail.content[].coinCount` | number | 코인 수 |
| `detail.content[].modelId` | integer | 모델 ID (없을 수 있음) |
| `detail.content[].createdAt` | string | 생성 시각 (ISO 8601) |

**오류 예시**
- **404** `ROOM_NOT_FOUND`
- **403** `FORBIDDEN`

#### 메시지 상세 조회
- **Method**: GET `/api/v1/messages/{messageId}`
- **인증**: 필수

**성공 응답 (200)**
```json
{
  "success": true,
  "detail": {
    "messageId": "uuid",
    "roomId": "uuid",
    "role": "assistant",
    "content": "string",
    "fileUrl": null,
    "tokenCount": 123.0,
    "coinCount": 0.000123,
    "modelId": 1,
    "createdAt": "2025-01-01T00:00:00Z"
  },
  "timestamp": "2025-01-01T00:00:00Z"
}
```

**응답 필드 (MessageResponse)**
| 필드 | 타입 | 설명 |
|---|---|---|
| `detail.messageId` | string | 메시지 UUID |
| `detail.roomId` | string | 채팅방 UUID |
| `detail.role` | string | 역할 (`user`, `assistant`) |
| `detail.content` | string | 메시지 내용 |
| `detail.fileUrl` | string | 첨부 파일 URL (없으면 `null`) |
| `detail.tokenCount` | number | 토큰 수 |
| `detail.coinCount` | number | 코인 수 |
| `detail.modelId` | integer | 모델 ID (없을 수 있음) |
| `detail.createdAt` | string | 생성 시각 (ISO 8601) |

**오류 예시**
- **404** `MESSAGE_NOT_FOUND`
- **403** `FORBIDDEN`

#### 메시지 전송 (SSE 스트리밍)
- **Method**: POST `/api/v1/messages/send/{roomId}`
- **인증**: 필수
- **Produces**: `text/event-stream`

**요청 본문**
```json
{
  "message": "안녕하세요!",
  "modelId": 1,
  "files": [
    {
      "id": "file-abc123",
      "type": "image"
    }
  ]
}
```

**요청 필드**
| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `message` | string | ✅ | 메시지 내용 |
| `modelId` | integer | ✅ | AI 모델 ID |
| `files` | array | ❌ | 첨부 파일 목록 |
| `files[].id` | string | ✅ | 업로드된 파일 ID |
| `files[].type` | string | ✅ | `image \| document \| audio` |

**SSE 이벤트**
1) `started` 이벤트 (문자열)
```
event: started
data: Message sending started
```

2) `response` 이벤트 (반복)
```json
{"type":"response","data":"텍스트 조각"}
```

3) `usage` 이벤트 (최종)
```json
{
  "type": "usage",
  "aiResponseId": "resp_abc123",
  "fullContent": "전체 응답 문자열",
  "usage": {
    "input_tokens": 10,
    "output_tokens": 20,
    "total_tokens": 30
  }
}
```

**클라이언트 참고**
- 브라우저 기본 `EventSource`는 **POST/요청 본문/커스텀 헤더를 지원하지 않으므로** 이 엔드포인트에 직접 사용하기 어렵습니다.
- `fetch`로 스트림을 읽어 SSE를 파싱하거나, POST를 지원하는 SSE 클라이언트(예: `@microsoft/fetch-event-source`)를 사용하세요.

**사용 예시 (TypeScript, fetch-event-source)**
```ts
import { fetchEventSource } from "@microsoft/fetch-event-source";

await fetchEventSource(`/api/v1/messages/send/${roomId}`, {
  method: "POST",
  headers: {
    "Content-Type": "application/json",
    Authorization: `Bearer ${accessToken}`,
  },
  body: JSON.stringify({ message, modelId, files }),
  onmessage(ev) {
    if (ev.event === "response") {
      const { data } = JSON.parse(ev.data) as { type: "response"; data: string };
      // data(텍스트 조각) 처리
    }
    if (ev.event === "usage") {
      const result = JSON.parse(ev.data);
      // 최종 결과 처리 (aiResponseId, fullContent, usage 등)
    }
  },
});
```

**오류**
- 스트리밍 시작 전 검증 단계에서 실패하면 일반 JSON(`ApiResponse<ErrorResponse>`)으로 반환될 수 있습니다.
- 스트리밍 중 오류는 연결 종료로 표현될 수 있습니다.

---

### 4. AI 모델 (AIModel)

#### 활성화된 AI 모델 목록 조회
- **Method**: GET `/api/v1/models`
- **인증**: 필수

**성공 응답 (200)**
```json
{
  "success": true,
  "detail": [
    {
      "modelId": 1,
      "modelName": "gpt-4",
      "displayName": "GPT-4",
      "displayExplain": "설명",
      "inputPricePer1m": 0.03,
      "outputPricePer1m": 0.06,
      "isActive": true,
      "createdAt": "2025-01-01T00:00:00Z",
      "updatedAt": "2025-01-01T00:00:00Z"
    }
  ],
  "timestamp": "2025-01-01T00:00:00Z"
}
```

**응답 필드 (AIModelResponse)**
| 필드 | 타입 | 설명 |
|---|---|---|
| `detail[].modelId` | integer | 모델 ID |
| `detail[].modelName` | string | 내부 모델 식별자 |
| `detail[].displayName` | string | 표시 이름 |
| `detail[].displayExplain` | string | 설명 |
| `detail[].inputPricePer1m` | number | 입력 1M 토큰당 가격 |
| `detail[].outputPricePer1m` | number | 출력 1M 토큰당 가격 |
| `detail[].isActive` | boolean | 활성화 여부 |
| `detail[].createdAt` | string | 생성 시각 (ISO 8601) |
| `detail[].updatedAt` | string | 수정 시각 (ISO 8601) |

**비고**
- `inputPricePer1m`, `outputPricePer1m`는 `modelMarkupRate`가 반영된 가격입니다.

---

### 5. 지갑 (UserWallet)

#### 지갑 상세 조회
- **Method**: GET `/api/v1/wallet`
- **인증**: 필수

**성공 응답 (200)**
```json
{
  "success": true,
  "detail": {
    "walletId": 1,
    "userId": 1,
    "balance": 100.5,
    "totalPurchased": 200.0,
    "totalUsed": 99.5,
    "lastTransactionAt": "2025-01-01T00:00:00Z",
    "createdAt": "2025-01-01T00:00:00Z",
    "updatedAt": "2025-01-01T00:00:00Z"
  },
  "timestamp": "2025-01-01T00:00:00Z"
}
```

**응답 필드 (UserWalletResponse)**
| 필드 | 타입 | 설명 |
|---|---|---|
| `detail.walletId` | integer | 지갑 ID |
| `detail.userId` | integer | 사용자 ID |
| `detail.balance` | number | 현재 잔액 |
| `detail.totalPurchased` | number | 누적 구매(충전) |
| `detail.totalUsed` | number | 누적 사용 |
| `detail.lastTransactionAt` | string | 마지막 거래 시각 (없으면 `null`) |
| `detail.createdAt` | string | 생성 시각 (ISO 8601) |
| `detail.updatedAt` | string | 수정 시각 (ISO 8601) |

**오류 예시**
- **404** `WALLET_NOT_FOUND`

#### 코인 잔액 조회
- **Method**: GET `/api/v1/wallet/balance`
- **인증**: 필수

**성공 응답 (200)**
```json
{
  "success": true,
  "detail": {
    "balance": 100.5
  },
  "timestamp": "2025-01-01T00:00:00Z"
}
```

**응답 필드 (BalanceResponse)**
| 필드 | 타입 | 설명 |
|---|---|---|
| `detail.balance` | number | 현재 잔액 |

**오류 예시**
- **404** `WALLET_NOT_FOUND`

---

### 6. 지갑 이력 (WalletHistory)

#### 지갑 이력 목록 조회
- **Method**: GET `/api/v1/payments`
- **인증**: 필수

**쿼리 파라미터**
| 파라미터 | 기본값 | 설명 |
|---|---:|---|
| `status` | - | 상태 필터(문자열) |
| `page` | 0 | 0부터 시작 |
| `size` | 20 | 페이지 크기 |

**성공 응답 (200)**: `detail`은 Spring Data `Page` 직렬화 형태입니다. 각 `content[]` 항목은 `WalletHistoryResponse`입니다.

**content[] 필드 (WalletHistoryResponse)**
| 필드 | 타입 | 설명 |
|---|---|---|
| `detail.content[].historyId` | integer | 지갑 이력 ID |
| `detail.content[].transactionId` | string | 트랜잭션 ID |
| `detail.content[].paymentMethod` | string | 결제 수단 |
| `detail.content[].amountKrw` | number | 결제 금액(KRW) |
| `detail.content[].amountUsd` | number | 환산 금액(USD) |
| `detail.content[].coinAmount` | number | 지급 코인 |
| `detail.content[].bonusCoin` | number | 보너스 코인 |
| `detail.content[].status` | string | 결제 상태(문자열) |
| `detail.content[].paymentGateway` | string | 결제 게이트웨이 |
| `detail.content[].metadata` | object | 부가 메타데이터 |
| `detail.content[].createdAt` | string | 생성 시각 (ISO 8601) |
| `detail.content[].completedAt` | string | 완료 시각 (없으면 `null`) |

#### 지갑 이력 상세 조회
- **Method**: GET `/api/v1/payments/{paymentId}`
- **인증**: 필수

**성공 응답 (200)** (`WalletHistoryResponse`)
```json
{
  "success": true,
  "detail": {
    "historyId": 1,
    "transactionId": "tx_123",
    "paymentMethod": "card",
    "amountKrw": 10000.0,
    "amountUsd": 7.5,
    "coinAmount": 7.5,
    "bonusCoin": 0.5,
    "status": "completed",
    "paymentGateway": "toss",
    "metadata": {},
    "createdAt": "2025-01-01T00:00:00Z",
    "completedAt": "2025-01-01T00:01:00Z"
  },
  "timestamp": "2025-01-01T00:00:00Z"
}
```

**오류 예시**
- **404** `WALLET_HISTORY_NOT_FOUND`
- **403** `FORBIDDEN`

---

### 7. 코인 거래 내역 (CoinTransaction)

#### 코인 거래 내역 조회
- **Method**: GET `/api/v1/transactions`
- **인증**: 필수

**쿼리 파라미터**
| 파라미터 | 기본값 | 설명 |
|---|---:|---|
| `transactionType` | - | 거래 유형(문자열) |
| `startDate` | - | `YYYY-MM-DD` |
| `endDate` | - | `YYYY-MM-DD` |
| `page` | 0 | 0부터 시작 |
| `size` | 20 | 페이지 크기 |

**성공 응답 (200)**: `detail`은 Spring Data `Page` 직렬화 형태입니다. 각 `content[]` 항목은 `CoinTransactionResponse`입니다.

**content[] 필드 (CoinTransactionResponse)**
| 필드 | 타입 | 설명 |
|---|---|---|
| `detail.content[].transactionId` | integer | 거래 ID |
| `detail.content[].transactionType` | string | 거래 유형(문자열) |
| `detail.content[].amount` | number | 증감액(코인) |
| `detail.content[].balanceAfter` | number | 거래 후 잔액 |
| `detail.content[].description` | string | 설명 |
| `detail.content[].modelId` | integer | 모델 ID (없을 수 있음) |
| `detail.content[].modelName` | string | 모델 이름 (없을 수 있음) |
| `detail.content[].roomId` | string | 채팅방 UUID (없을 수 있음) |
| `detail.content[].messageId` | string | 메시지 UUID (없을 수 있음) |
| `detail.content[].createdAt` | string | 생성 시각 (ISO 8601) |

---

### 8. 대시보드 (Dashboard)

#### 모델 가격 조회
- **Method**: GET `/api/v1/dashboard/models/pricing`
- **인증**: 필수

**성공 응답 (200)** (`ModelPricingResponse[]`)
```json
{
  "success": true,
  "detail": [
    {
      "modelId": 1,
      "modelName": "gpt-4",
      "displayName": "GPT-4",
      "inputPricePer1m": 0.03,
      "outputPricePer1m": 0.06,
      "isActive": true
    }
  ],
  "timestamp": "2025-01-01T00:00:00Z"
}
```

**응답 필드 (ModelPricingResponse)**
| 필드 | 타입 | 설명 |
|---|---|---|
| `detail[].modelId` | integer | 모델 ID |
| `detail[].modelName` | string | 내부 모델 식별자 |
| `detail[].displayName` | string | 표시 이름 |
| `detail[].inputPricePer1m` | number | 입력 1M 토큰당 가격 |
| `detail[].outputPricePer1m` | number | 출력 1M 토큰당 가격 |
| `detail[].isActive` | boolean | 활성화 여부 |

**비고**
- `inputPricePer1m`, `outputPricePer1m`는 `modelMarkupRate`가 반영된 가격입니다.

#### 월별 사용량 조회
- **Method**: GET `/api/v1/dashboard/usage/monthly`
- **인증**: 필수

**쿼리 파라미터**
| 파라미터 | 기본값 | 설명 |
|---|---|---|
| `year` | 현재 연도 | 조회할 연도 |
| `month` | 현재 월 | 1~12 |

**성공 응답 (200)** (`MonthlyUsageResponse`)
```json
{
  "success": true,
  "detail": {
    "year": 2025,
    "month": 1,
    "totalCoinUsed": 50.5,
    "modelUsage": [
      {
        "modelId": 1,
        "modelName": "gpt-4",
        "displayName": "GPT-4",
        "coinUsed": 30.0,
        "messageCount": 150,
        "tokenCount": 50000.0,
        "percentage": 59.4
      }
    ],
    "dailyUsage": [
      {
        "date": "2025-01-01",
        "coinUsed": 2.5,
        "messageCount": 10
      }
    ]
  },
  "timestamp": "2025-01-01T00:00:00Z"
}
```

**응답 필드 (MonthlyUsageResponse)**
| 필드 | 타입 | 설명 |
|---|---|---|
| `detail.year` | integer | 연도 |
| `detail.month` | integer | 월 |
| `detail.totalCoinUsed` | number | 총 코인 사용량 |
| `detail.modelUsage[]` | array | 모델별 사용량 |
| `detail.dailyUsage[]` | array | 일별 사용량 |

**모델별 상세 (ModelUsageDetail)**
| 필드 | 타입 | 설명 |
|---|---|---|
| `detail.modelUsage[].modelId` | integer | 모델 ID |
| `detail.modelUsage[].modelName` | string | 모델 이름 |
| `detail.modelUsage[].displayName` | string | 표시 이름 |
| `detail.modelUsage[].coinUsed` | number | 사용 코인 |
| `detail.modelUsage[].messageCount` | integer | 메시지 수 |
| `detail.modelUsage[].tokenCount` | number | 토큰 수 |
| `detail.modelUsage[].percentage` | number | 비율(%) |

**일별 상세 (DailyUsageDetail)**
| 필드 | 타입 | 설명 |
|---|---|---|
| `detail.dailyUsage[].date` | string | 날짜 (`YYYY-MM-DD`) |
| `detail.dailyUsage[].coinUsed` | number | 사용 코인 |
| `detail.dailyUsage[].messageCount` | integer | 메시지 수 |

#### 사용자 통계 조회
- **Method**: GET `/api/v1/dashboard/stats`
- **인증**: 필수

**성공 응답 (200)** (`UserStatsResponse`)
```json
{
  "success": true,
  "detail": {
    "totalCoinPurchased": 200.0,
    "totalCoinUsed": 99.5,
    "currentBalance": 100.5,
    "totalMessages": 500,
    "totalChatRooms": 20,
    "mostUsedModel": {
      "modelId": 1,
      "modelName": "gpt-4",
      "displayName": "GPT-4",
      "usagePercentage": 60.0
    },
    "last30DaysUsage": 25.5,
    "memberSince": "2025-01-01T00:00:00Z"
  },
  "timestamp": "2025-01-01T00:00:00Z"
}
```

**응답 필드 (UserStatsResponse)**
| 필드 | 타입 | 설명 |
|---|---|---|
| `detail.totalCoinPurchased` | number | 누적 구매(충전) |
| `detail.totalCoinUsed` | number | 누적 사용 |
| `detail.currentBalance` | number | 현재 잔액 |
| `detail.totalMessages` | integer | 메시지 수 |
| `detail.totalChatRooms` | integer | 채팅방 수 |
| `detail.mostUsedModel` | object | 최다 사용 모델(없으면 `null`) |
| `detail.last30DaysUsage` | number | 최근 30일 사용량 |
| `detail.memberSince` | string | 가입 시각 (ISO 8601) |

**최다 사용 모델 (MostUsedModel)**
| 필드 | 타입 | 설명 |
|---|---|---|
| `detail.mostUsedModel.modelId` | integer | 모델 ID |
| `detail.mostUsedModel.modelName` | string | 모델 이름 |
| `detail.mostUsedModel.displayName` | string | 표시 이름 |
| `detail.mostUsedModel.usagePercentage` | number | 사용 비율(%) |

---

### 9. 관리자 - 사용자 관리 (Admin User Management)

#### [관리자] 사용자 권한 수정
- **Method**: PATCH `/api/v1/admin/authority`
- **인증**: 필수 (ADMIN)
- **설명**: 관리자가 다른 사용자의 권한을 수정합니다. 본인의 권한은 수정할 수 없습니다.

**요청 본문**
```json
{
  "userId": 2,
  "role": "ADMIN"
}
```

**요청 제약**
| 필드 | 제약 |
|---|---|
| `userId` | 필수, 본인 ID 불가 |
| `role` | 필수, `USER` 또는 `ADMIN` |

**성공 응답 (200)** (본문은 `detail: null`)
```json
{
  "success": true,
  "detail": null,
  "timestamp": "2025-01-01T00:00:00Z"
}
```

**오류**
- **403 Forbidden**: 권한 부족 또는 본인 권한 수정 시도 시 `ErrorResponse` 단독 객체로 응답될 수 있음
- **404** `USER_NOT_FOUND`

#### [관리자] 전체 사용자 정보 조회
- **Method**: GET `/api/v1/admin/users`
- **인증**: 필수 (ADMIN)
- **설명**: 모든 사용자의 기본 정보와 지갑 정보를 조회합니다.

**성공 응답 (200)**
```json
{
  "success": true,
  "detail": [
    {
      "userId": 1,
      "username": "홍길동",
      "role": "USER",
      "email": "user@example.com",
      "walletId": 1,
      "balance": 100.5,
      "paidBalance": 90.0,
      "promotionBalance": 10.5,
      "totalPurchased": 200.0,
      "totalUsed": 99.5
    }
  ],
  "timestamp": "2025-01-01T00:00:00Z"
}
```

**응답 필드 (UserListResponse)**
| 필드 | 타입 | 설명 |
|---|---|---|
| `detail[].userId` | integer | 사용자 ID |
| `detail[].username` | string | 사용자 이름 |
| `detail[].role` | string | 사용자 권한 (`USER`, `ADMIN`) |
| `detail[].email` | string | 이메일 |
| `detail[].walletId` | integer | 지갑 ID (없으면 `null`) |
| `detail[].balance` | number | 총 잔액 |
| `detail[].paidBalance` | number | 유료 코인 잔액 |
| `detail[].promotionBalance` | number | 프로모션 코인 잔액 |
| `detail[].totalPurchased` | number | 누적 구매(충전) |
| `detail[].totalUsed` | number | 누적 사용 |

**오류**
- **403 Forbidden**: 권한 부족 시 `ErrorResponse` 단독 객체로 응답될 수 있음

#### [관리자] 사용자 프로모션 코인 수정
- **Method**: PATCH `/api/v1/admin/wallet`
- **인증**: 필수 (ADMIN)
- **설명**: 관리자가 사용자의 프로모션 코인을 증감합니다. 변경 이력은 WalletHistory(wallet_history)에 자동으로 기록됩니다.

**요청 본문**
```json
{
  "userId": 2,
  "promotionBalance": 50.0
}
```

**요청 제약**
| 필드 | 제약 |
|---|---|
| `userId` | 필수 |
| `promotionBalance` | 필수, 양수는 증가, 음수는 감소 |

**비고**
- 프로모션 코인이 0 미만으로 내려갈 수 없습니다.
- 변경 이력은 자동으로 `WalletHistory` 테이블에 기록됩니다.

**성공 응답 (200)** (본문은 `detail: null`)
```json
{
  "success": true,
  "detail": null,
  "timestamp": "2025-01-01T00:00:00Z"
}
```

**오류**
- **403 Forbidden**: 권한 부족 시 `ErrorResponse` 단독 객체로 응답될 수 있음
- **404** `WALLET_NOT_FOUND`
- **400** `VALIDATION_ERROR`: 프로모션 코인이 0 미만으로 내려가는 경우

**403 응답 예시 (ErrorResponse 단독)**
```json
{
  "code": "FORBIDDEN",
  "message": "접근 권한이 없습니다"
}
```

---

### 10. 관리자 - AI 모델 관리 (Admin Model Management)

#### [관리자] AI 모델 등록
- **Method**: POST `/api/v1/admin/models`
- **인증**: 필수 (ADMIN)

**요청 본문**
```json
{
  "modelName": "gpt-4-turbo",
  "displayName": "GPT-4 Turbo",
  "displayExplain": "설명",
  "inputPricePer1m": 0.01,
  "outputPricePer1m": 0.03,
  "modelMarkupRate": 0.2,
  "isActive": true
}
```

**요청 제약**
| 필드 | 제약 |
|---|---|
| `modelName` | 정규식 `^[a-z0-9.-]+$` |
| `displayName` | 최대 30자 |
| `displayExplain` | 최대 200자 |
| `inputPricePer1m` / `outputPricePer1m` | 0 이상 |
| `modelMarkupRate` | 0 이상 |
| `isActive` | 필수 |

**성공 응답 (201)**: `AIModelResponse`
```json
{
  "success": true,
  "detail": {
    "modelId": 3,
    "modelName": "gpt-4-turbo",
    "displayName": "GPT-4 Turbo",
    "displayExplain": "설명",
    "inputPricePer1m": 0.01,
    "outputPricePer1m": 0.03,
    "isActive": true,
    "createdAt": "2025-01-01T00:00:00Z",
    "updatedAt": "2025-01-01T00:00:00Z"
  },
  "timestamp": "2025-01-01T00:00:00Z"
}
```

#### [관리자] AI 모델 상세 조회
- **Method**: GET `/api/v1/admin/models/{modelId}`
- **인증**: 필수 (ADMIN)

**성공 응답 (200)**: `AIModelDetailResponse`
```json
{
  "success": true,
  "detail": {
    "modelId": 1,
    "modelName": "gpt-4",
    "displayName": "GPT-4",
    "displayExplain": "설명",
    "inputPricePer1m": 0.02,
    "outputPricePer1m": 0.04,
    "modelMarkupRate": 0.2,
    "isActive": true,
    "createdAt": "2025-01-01T00:00:00Z",
    "updatedAt": "2025-01-01T00:00:00Z"
  },
  "timestamp": "2025-01-01T00:00:00Z"
}
```

**응답 필드 (AIModelDetailResponse)**
| 필드 | 타입 | 설명 |
|---|---|---|
| `detail.modelId` | integer | 모델 ID |
| `detail.modelName` | string | 내부 모델 식별자 |
| `detail.displayName` | string | 표시 이름 |
| `detail.displayExplain` | string | 설명 |
| `detail.inputPricePer1m` | number | 입력 1M 토큰당 원가 |
| `detail.outputPricePer1m` | number | 출력 1M 토큰당 원가 |
| `detail.modelMarkupRate` | number | 마크업 비율 |
| `detail.isActive` | boolean | 활성화 여부 |
| `detail.createdAt` | string | 생성 시각 (ISO 8601) |
| `detail.updatedAt` | string | 수정 시각 (ISO 8601) |

**비고**
- `inputPricePer1m`, `outputPricePer1m`는 마크업 적용 전 원가입니다.
- 마크업 적용 가격은 `원가 * (1 + modelMarkupRate)`입니다.

#### [관리자] AI 모델 수정
- **Method**: PUT `/api/v1/admin/models/{modelId}`
- **인증**: 필수 (ADMIN)
- **비고**: 요청 본문의 필드는 모두 선택이며, `null`이 아닌 값만 반영됩니다.

**요청 본문 예시**
```json
{
  "displayName": "새 표시 이름",
  "displayExplain": "새 설명",
  "inputPricePer1m": 0.02,
  "outputPricePer1m": 0.05,
  "modelMarkupRate": 0.1,
  "isActive": true
}
```

**성공 응답 (200)**: `AIModelResponse`

#### [관리자] AI 모델 삭제(비활성화)
- **Method**: DELETE `/api/v1/admin/models/{modelId}`
- **인증**: 필수 (ADMIN)
- **성공**: `204 No Content`
