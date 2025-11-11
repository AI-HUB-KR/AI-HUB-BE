## API 응답 구조 표준

모든 API는 일관된 구조의 응답을 반환합니다.

### 성공 응답 구조
```json
{
  "success": true,
  "detail": {
    // 실제 응답 데이터
  },
  "timestamp": "2025-01-01T00:00:00Z"
}
```

**설명**:
- `success`: 요청 성공 여부 (true)
- `detail`: 실제 응답 데이터 객체
- `timestamp`: 응답 생성 시각 (ISO 8601 형식)

### 실패 응답 구조
```json
{
  "success": false,
  "detail": {
    "code": "ERROR_CODE",
    "message": "오류 메시지",
    "details": "상세 오류 정보 (선택적)"
  },
  "timestamp": "2025-01-01T00:00:00Z"
}
```

**설명**:
- `success`: 요청 성공 여부 (false)
- `detail`: 에러 정보 객체 (ErrorResponse)
  - `code`: 에러 코드 (Enum 이름)
  - `message`: 사용자에게 표시할 에러 메시지
  - `details`: 추가 상세 정보 (선택적, null 가능)
- `timestamp`: 응답 생성 시각 (ISO 8601 형식)

### HTTP Status Code 정의

#### 성공 응답 (2xx)
- **200 OK**: 요청 성공
- **201 Created**: 리소스 생성 성공
- **204 No Content**: 요청 성공, 응답 본문 없음 (삭제 등)

#### 클라이언트 오류 (4xx)
- **400 Bad Request**: 잘못된 요청 (validation 실패)
- **401 Unauthorized**: 인증 실패 (로그인 필요)
- **402 Payment Required**: 결제 필요 (코인 잔액 부족 등)
- **403 Forbidden**: 권한 없음
- **404 Not Found**: 리소스를 찾을 수 없음
- **409 Conflict**: 리소스 충돌 (중복 등)

#### 서버 오류 (5xx)
- **500 Internal Server Error**: 서버 내부 오류
- **503 Service Unavailable**: 서비스 이용 불가

### 인증 헤더 형식

토큰 기반 인증이 필요한 모든 엔드포인트는 아래 형식의 헤더를 포함해야 합니다.

```http
Authorization: Bearer <ACCESS_TOKEN>
```

리프레시 토큰을 쿠키에 담아 전송하는 경우 별도의 본문은 필요하지 않습니다.

### 에러 코드 정의

**보안 원칙**: 인증 전 API는 일반화된 에러 코드를 사용하여 시스템 내부 정보 노출을 방지합니다.

#### 인증 전 (Public API)

| 코드 | 설명 | 사용 예시 |
|------|------|----------|
| `AUTHENTICATION_FAILED` | 인증 실패 (사용자 열거 방지) | 로그인 실패, 토큰 검증 실패 |
| `VALIDATION_ERROR` | 입력값 검증 오류 (구체적 정보 최소화) | 회원가입 중복 검사, 형식 오류 |

#### 인증 후 (Authenticated API)

| 코드 | 설명 |
|------|------|
| `INSUFFICIENT_BALANCE` | 코인 잔액 부족 |
| `ROOM_NOT_FOUND` | 채팅방을 찾을 수 없음 |
| `MESSAGE_NOT_FOUND` | 메시지를 찾을 수 없음 |
| `MODEL_NOT_FOUND` | AI 모델을 찾을 수 없음 |
| `PAYMENT_FAILED` | 결제 실패 |
| `PAYMENT_NOT_FOUND` | 결제 내역을 찾을 수 없음 |
| `WALLET_NOT_FOUND` | 지갑을 찾을 수 없음 |
| `INVALID_TOKEN` | 유효하지 않은 토큰 |
| `FORBIDDEN` | 권한 없음 (타 사용자의 리소스 접근 시도) |
| `TRANSACTION_NOT_FOUND` | 거래 내역을 찾을 수 없음 |
| `CONFLICT` | 리소스 충돌 (중복, 모순 등) |
| `TOKEN_REUSED` | 회전된 리프레시 토큰 재사용 감지 |
| `SYSTEM_ILLEGAL_STATE` | 내부 시스템 상태 불일치 (토큰 해싱 실패 등) |

#### 토큰 폐기 사유 (TokenRevokeReason)

에러 응답의 `details` 필드에 포함될 수 있는 토큰 폐기 사유:

| 사유 | 설명 | 발생 상황 |
|------|------|----------|
| `ROTATED` | 토큰 로테이션으로 인한 폐기 | 토큰 갱신 API 호출 시 기존 토큰 폐기 |
| `EXPIRED` | 토큰 만료로 인한 폐기 | 유효 기간이 지난 토큰 접근 시도 |
| `USER_LOGOUT` | 사용자 로그아웃으로 인한 폐기 | 로그아웃 API 호출 시 토큰 폐기 |

**사용 예시**:
- 토큰 갱신 시 동시성 문제로 이미 회전된 토큰 재사용 감지:
  ```json
  {
    "code": "TOKEN_REUSED",
    "message": "이미 회전된 리프레시 토큰입니다.",
    "details": "TokenRevokeReason.ROTATED"
  }
  ```
- 로그아웃 후 토큰으로 요청:
  ```json
  {
    "code": "AUTHENTICATION_FAILED",
    "message": "유효하지 않은 토큰입니다.",
    "details": "TokenRevokeReason.USER_LOGOUT"
  }
  ```

## API 엔드포인트

### 1. 사용자 관리 (User)

#### 내 정보 조회
- **Method**: GET `/api/v1/users/me`
- **설명**: 현재 로그인한 사용자 정보를 조회합니다.
- **인증**: 필수 (Bearer Token)

**요청 헤더**
```http
Authorization: Bearer <ACCESS_TOKEN>
```

**성공 응답**
- **200 OK**
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

**응답 필드**

| 필드 | 타입 | 설명 |
|------|------|------|
| success | boolean | 요청 처리 성공 여부 |
| detail.userId | integer | 사용자 식별자 |
| detail.username | string | 사용자 이름 |
| detail.email | string | 사용자 이메일 |
| detail.isActivated | boolean | 계정 활성화 여부 |
| detail.createdAt | string | 계정 생성 시각 (ISO 8601) |
| timestamp | string | 응답 생성 시각 (ISO 8601) |

**오류 응답 예시**
- **401 Unauthorized**: 유효하지 않은 액세스 토큰
  ```json
  {
    "success": false,
    "detail": {
      "code": "INVALID_TOKEN",
      "message": "인증 정보가 유효하지 않습니다.",
      "details": null
    },
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```
- **403 Forbidden**: 비활성화된 계정
  ```json
  {
    "success": false,
    "detail": {
      "code": "FORBIDDEN",
      "message": "접근 권한이 없습니다.",
      "details": "계정이 비활성화되었습니다."
    },
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```

#### 내 정보 수정
- **Method**: PUT `/api/v1/users/me`
- **설명**: 현재 로그인한 사용자의 정보를 수정합니다.
- **인증**: 필수 (Bearer Token)

**요청 헤더**
```http
Authorization: Bearer <ACCESS_TOKEN>
Content-Type: application/json
```

**요청 본문**
```json
{
  "username": "string",
  "email": "string"
}
```

**요청 필드**

| 필드 | 타입 | 필수 | 설명 | 제약사항 |
|------|------|------|------|----------|
| username | string | ✅ | 사용자 이름 | 2~30자, 공백 불가 |
| email | string | ✅ | 사용자 이메일 | RFC 5322 형식 |

**성공 응답**
- **200 OK**
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

**응답 필드**: `내 정보 조회`와 동일

**오류 응답 예시**
- **400 Bad Request**: 검증 실패
  ```json
  {
    "success": false,
    "detail": {
      "code": "VALIDATION_ERROR",
      "message": "요청 필드가 올바르지 않습니다.",
      "details": "username은 2자 이상이어야 합니다."
    },
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```
- **409 Conflict**: 이메일 중복
  ```json
  {
    "success": false,
    "detail": {
      "code": "VALIDATION_ERROR",
      "message": "이미 사용 중인 이메일입니다.",
      "details": null
    },
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```

#### 회원 탈퇴
- **Method**: DELETE `/api/v1/users/me`
- **설명**: 회원을 소프트 삭제 처리합니다.
- **인증**: 필수 (Bearer Token)

**요청 헤더**
```http
Authorization: Bearer <ACCESS_TOKEN>
```

**성공 응답**
- **204 No Content**: 별도의 본문 없음

**오류 응답 예시**
- **401 Unauthorized**: 토큰 누락 또는 만료
  ```json
  {
    "success": false,
    "detail": {
      "code": "INVALID_TOKEN",
      "message": "인증이 필요합니다.",
      "details": null
    },
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```
- **403 Forbidden**: 다른 사용자의 리소스 접근 시도
  ```json
  {
    "success": false,
    "detail": {
      "code": "FORBIDDEN",
      "message": "접근 권한이 없습니다.",
      "details": null
    },
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```

### 2. 인증 (Auth - 카카오 소셜 로그인)

#### 카카오 소셜 로그인 시작
- **Method**: GET `/oauth2/authorization/kakao`
- **설명**: 카카오 OAuth2 인증 페이지로 브라우저를 리다이렉트합니다. 최초 로그인 시 자동 회원 가입이 이루어집니다.
- **인증**: Public

**성공 응답**
- **302 Found**: 카카오 로그인 페이지로 리다이렉트

**오류 응답 예시**
- **503 Service Unavailable**: 카카오 OAuth 서버 장애
  ```json
  {
    "success": false,
    "detail": {
      "code": "SYSTEM_ILLEGAL_STATE",
      "message": "소셜 로그인 제공자와 통신할 수 없습니다.",
      "details": "카카오 OAuth 서버 응답 지연"
    },
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```

**참고**: `/login/oauth2/code/kakao` 콜백은 Spring Security 내부 흐름에서만 사용되며, 클라이언트가 직접 호출할 필요가 없습니다.

#### 토큰 갱신
- **Method**: POST `/api/v1/token/refresh`
- **설명**: `refreshToken` 쿠키를 사용해 새로운 액세스 토큰과 리프레시 토큰을 회전 발급합니다.
- **인증**: 필수 (Refresh Token Cookie)

**요청 헤더**
```http
Content-Type: application/json
Cookie: refreshToken=<REFRESH_TOKEN>
```

**요청 본문**: 없음 (쿠키 기반 인증)

**성공 응답**
- **200 OK**
  ```json
  {
    "success": true,
    "detail": null,
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```

  **응답 쿠키** (자동 전송, 클라이언트 처리 불필요):
  ```http
  Set-Cookie: accessToken=<NEW_ACCESS_TOKEN>; HttpOnly; Secure; SameSite=Strict; Path=/; Max-Age=3600
  Set-Cookie: refreshToken=<NEW_REFRESH_TOKEN>; HttpOnly; Secure; SameSite=Strict; Path=/; Max-Age=604800
  ```

**응답 특징**

| 항목 | 설명 |
|------|------|
| 응답 본문 | 비어있음 (detail: null) - 토큰은 쿠키로만 전송 |
| accessToken 쿠키 | 3600초(1시간) 유효, 모든 API 요청 시 자동 전송 |
| refreshToken 쿠키 | 604800초(7일) 유효, 토큰 갱신 시에만 사용 |
| HttpOnly 설정 | XSS 공격 방지 - JavaScript에서 접근 불가 |
| Secure 설정 | HTTPS 연결에서만 전송 |
| SameSite 설정 | CSRF 공격 방지 |

**오류 응답 예시**
- **401 Unauthorized**: 만료되었거나 폐기된 리프레시 토큰
  ```json
  {
    "success": false,
    "detail": {
      "code": "AUTHENTICATION_FAILED",
      "message": "리프레시 토큰이 만료되었거나 폐기되었습니다.",
      "details": null
    },
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```
- **409 Conflict**: 동시에 회전된 토큰 재사용 감지
  ```json
  {
    "success": false,
    "detail": {
      "code": "TOKEN_REUSED",
      "message": "이미 회전된 리프레시 토큰입니다.",
      "details": "TokenRevokeReason.ROTATED"
    },
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```

**비고**:
- 서버는 전달된 리프레시 토큰을 SHA-256 해시 후 DB와 대조합니다
- 검증이 완료된 기존 토큰은 `TokenRevokeReason.ROTATED` 상태로 폐기됩니다
- 토큰은 응답 본문이 아닌 **쿠키에만 포함**되어 XSS 공격으로부터 안전합니다
- 클라이언트는 쿠키를 자동으로 관리하므로, 별도의 토큰 저장 로직이 필요 없습니다

#### 로그아웃
- **Method**: POST `/api/v1/auth/logout`
- **설명**: 액세스 토큰과 리프레시 토큰을 폐기하고 로그아웃 처리합니다.
- **인증**: 필수 (Bearer Token)

**요청 헤더**
```http
Authorization: Bearer <ACCESS_TOKEN>
Content-Type: application/json
```

**요청 본문**
```json
{
  "refreshToken": "string"
}
```

**요청 필드**

| 필드 | 타입 | 필수 | 설명 | 제약사항 |
|------|------|------|------|----------|
| refreshToken | string | ✅ | 폐기할 리프레시 토큰 원문 | 만료되지 않은 토큰 |

**성공 응답**
- **204 No Content**

**오류 응답 예시**
- **400 Bad Request**: 요청 본문 누락
  ```json
  {
    "success": false,
    "detail": {
      "code": "VALIDATION_ERROR",
      "message": "refreshToken 필드는 필수입니다.",
      "details": null
    },
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```
- **401 Unauthorized**: 액세스 토큰 누락 또는 만료
  ```json
  {
    "success": false,
    "detail": {
      "code": "INVALID_TOKEN",
      "message": "인증이 필요합니다.",
      "details": null
    },
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```

**처리**: 검증에 성공하면 관련 액세스 토큰과 리프레시 토큰이 `TokenRevokeReason.USER_LOGOUT` 사유로 폐기됩니다.

### 3. 채팅방 관리 (ChatRoom)

#### 채팅방 생성
- **Method**: POST `/api/v1/chat-rooms`
- **설명**: 새로운 채팅방을 생성합니다.
- **인증**: 필수 (Bearer Token)

**요청 헤더**
```http
Authorization: Bearer <ACCESS_TOKEN>
Content-Type: application/json
```

**요청 본문**
```json
{
  "title": "새로운 채팅방",
  "modelId": 1
}
```

**요청 필드**

| 필드 | 타입 | 필수 | 설명 | 제약사항 |
|------|------|------|------|----------|
| title | string | ✅ | 채팅방 제목 | 최대 30자, 공백만 입력 불가 |
| modelId | integer | ✅ | 사용할 AI 모델 ID | 활성 상태의 모델만 허용 |

**성공 응답**
- **201 Created**
  ```json
  {
    "success": true,
    "detail": {
      "roomId": "uuid-v7",
      "title": "새로운 채팅방",
      "userId": 1,
      "coinUsage": 0.0,
      "createdAt": "2025-01-01T00:00:00Z",
      "updatedAt": "2025-01-01T00:00:00Z"
    },
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```

**응답 필드**

| 필드 | 타입 | 설명 |
|------|------|------|
| detail.roomId | string | 생성된 채팅방 UUID (v7) |
| detail.title | string | 채팅방 제목 |
| detail.userId | integer | 채팅방 소유 사용자 ID |
| detail.coinUsage | number | 누적 코인 사용량 |
| detail.createdAt | string | 생성 시각 (ISO 8601) |
| detail.updatedAt | string | 수정 시각 (ISO 8601) |

**오류 응답 예시**
- **400 Bad Request**: 요청 필드 검증 실패
  ```json
  {
    "success": false,
    "detail": {
      "code": "VALIDATION_ERROR",
      "message": "채팅방 제목은 30자 이하여야 합니다.",
      "details": null
    },
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```
- **404 Not Found**: 모델을 찾을 수 없음
  ```json
  {
    "success": false,
    "detail": {
      "code": "MODEL_NOT_FOUND",
      "message": "지정한 AI 모델이 존재하지 않습니다.",
      "details": null
    },
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```

#### 채팅방 목록 조회
- **Method**: GET `/api/v1/chat-rooms`
- **설명**: 현재 로그인한 사용자의 채팅방 목록을 페이지네이션하여 조회합니다.
- **인증**: 필수 (Bearer Token)

**요청 헤더**
```http
Authorization: Bearer <ACCESS_TOKEN>
```

**쿼리 파라미터**

| 파라미터 | 타입 | 기본값 | 설명 | 제약사항 |
|---------|------|--------|------|----------|
| page | integer | 0 | 페이지 번호 | 0 이상 |
| size | integer | 20 | 페이지 크기 | 1~100 |
| sort | string | createdAt,desc | 정렬 조건 | `필드,방향` 형식 |

**성공 응답**
- **200 OK**
  ```json
  {
    "success": true,
    "detail": {
      "content": [
        {
          "roomId": "uuid-v7",
          "title": "채팅방 제목",
          "coinUsage": 10.5,
          "lastMessageAt": "2025-01-01T00:00:00Z",
          "createdAt": "2025-01-01T00:00:00Z"
        }
      ],
      "totalElements": 100,
      "totalPages": 5,
      "size": 20,
      "number": 0
    },
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```

**응답 필드**

| 필드 | 타입 | 설명 |
|------|------|------|
| detail.content[].roomId | string | 채팅방 UUID |
| detail.content[].title | string | 채팅방 제목 |
| detail.content[].coinUsage | number | 누적 코인 사용량 |
| detail.content[].lastMessageAt | string | 마지막 메시지 시각 |
| detail.content[].createdAt | string | 채팅방 생성 시각 |
| detail.totalElements | integer | 전체 채팅방 수 |
| detail.totalPages | integer | 전체 페이지 수 |
| detail.size | integer | 페이지 크기 |
| detail.number | integer | 현재 페이지 번호 |

**오류 응답 예시**
- **401 Unauthorized**: 인증 정보 누락
  ```json
  {
    "success": false,
    "detail": {
      "code": "INVALID_TOKEN",
      "message": "인증이 필요합니다.",
      "details": null
    },
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```
- **400 Bad Request**: 잘못된 정렬 파라미터
  ```json
  {
    "success": false,
    "detail": {
      "code": "VALIDATION_ERROR",
      "message": "지원하지 않는 정렬 필드입니다.",
      "details": "sort=unknown,asc"
    },
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```

#### 채팅방 상세 조회
- **Method**: GET `/api/v1/chat-rooms/{roomId}`
- **설명**: 특정 채팅방에 대한 상세 정보를 조회합니다.
- **인증**: 필수 (Bearer Token)

**요청 헤더**
```http
Authorization: Bearer <ACCESS_TOKEN>
```

**경로 변수**

| 변수 | 타입 | 설명 |
|------|------|------|
| roomId | string | 조회할 채팅방 UUID |

**성공 응답**
- **200 OK**
  ```json
  {
    "success": true,
    "detail": {
      "roomId": "uuid-v7",
      "title": "채팅방 제목",
      "userId": 1,
      "coinUsage": 10.5,
      "createdAt": "2025-01-01T00:00:00Z",
      "updatedAt": "2025-01-01T00:00:00Z"
    },
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```

**응답 필드**

| 필드 | 타입 | 설명 |
|------|------|------|
| detail.roomId | string | 채팅방 UUID |
| detail.title | string | 채팅방 제목 |
| detail.userId | integer | 채팅방 소유자 ID |
| detail.coinUsage | number | 누적 코인 사용량 |
| detail.createdAt | string | 생성 시각 |
| detail.updatedAt | string | 수정 시각 |

**오류 응답 예시**
- **404 Not Found**: 채팅방 없음
  ```json
  {
    "success": false,
    "detail": {
      "code": "ROOM_NOT_FOUND",
      "message": "채팅방이 존재하지 않습니다.",
      "details": "roomId=92c3..."
    },
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```
- **403 Forbidden**: 다른 사용자의 채팅방 접근 시도
  ```json
  {
    "success": false,
    "detail": {
      "code": "FORBIDDEN",
      "message": "접근 권한이 없습니다.",
      "details": null
    },
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```

#### 채팅방 제목 수정
- **Method**: PUT `/api/v1/chat-rooms/{roomId}`
- **설명**: 채팅방 제목을 수정합니다.
- **인증**: 필수 (Bearer Token)

**요청 헤더**
```http
Authorization: Bearer <ACCESS_TOKEN>
Content-Type: application/json
```

**경로 변수**

| 변수 | 타입 | 설명 |
|------|------|------|
| roomId | string | 수정할 채팅방 UUID |

**요청 본문**
```json
{
  "title": "수정된 채팅방 제목"
}
```

**요청 필드**

| 필드 | 타입 | 필수 | 설명 | 제약사항 |
|------|------|------|------|----------|
| title | string | ✅ | 새로운 채팅방 제목 | 최대 30자 |

**성공 응답**
- **200 OK**
  ```json
  {
    "success": true,
    "detail": {
      "roomId": "uuid-v7",
      "title": "수정된 채팅방 제목",
      "userId": 1,
      "coinUsage": 10.5,
      "createdAt": "2025-01-01T00:00:00Z",
      "updatedAt": "2025-01-02T00:00:00Z"
    },
    "timestamp": "2025-01-02T00:00:00Z"
  }
  ```

**응답 필드**

| 필드 | 타입 | 설명 |
|------|------|------|
| detail.roomId | string | 채팅방 UUID |
| detail.title | string | 채팅방 제목 |
| detail.userId | integer | 채팅방 소유자 ID |
| detail.coinUsage | number | 누적 코인 사용량 |
| detail.createdAt | string | 생성 시각 |
| detail.updatedAt | string | 수정 시각 |

**오류 응답 예시**
- **400 Bad Request**: 제목 길이 초과
  ```json
  {
    "success": false,
    "detail": {
      "code": "VALIDATION_ERROR",
      "message": "채팅방 제목은 30자 이하여야 합니다.",
      "details": null
    },
    "timestamp": "2025-01-02T00:00:00Z"
  }
  ```
- **404 Not Found**: 채팅방 없음
  ```json
  {
    "success": false,
    "detail": {
      "code": "ROOM_NOT_FOUND",
      "message": "채팅방이 존재하지 않습니다.",
      "details": null
    },
    "timestamp": "2025-01-02T00:00:00Z"
  }
  ```

#### 채팅방 삭제
- **Method**: DELETE `/api/v1/chat-rooms/{roomId}`
- **설명**: 채팅방을 삭제하며 연관 메시지도 함께 삭제합니다.
- **인증**: 필수 (Bearer Token)

**요청 헤더**
```http
Authorization: Bearer <ACCESS_TOKEN>
```

**경로 변수**

| 변수 | 타입 | 설명 |
|------|------|------|
| roomId | string | 삭제할 채팅방 UUID |

**성공 응답**
- **204 No Content**

**오류 응답 예시**
- **404 Not Found**: 존재하지 않는 채팅방
  ```json
  {
    "success": false,
    "detail": {
      "code": "ROOM_NOT_FOUND",
      "message": "채팅방이 존재하지 않습니다.",
      "details": null
    },
    "timestamp": "2025-01-02T00:00:00Z"
  }
  ```
- **403 Forbidden**: 다른 사용자의 채팅방 삭제 시도
  ```json
  {
    "success": false,
    "detail": {
      "code": "FORBIDDEN",
      "message": "접근 권한이 없습니다.",
      "details": null
    },
    "timestamp": "2025-01-02T00:00:00Z"
  }
  ```

### 4. 파일 업로드 (File Upload)

#### 파일 업로드
- **Method**: POST `/api/v1/files/upload`
- **설명**: 사용자가 업로드한 파일을 Cloudflare R2에 저장하고 접근 가능한 URL을 반환합니다.
- **인증**: 필수 (Bearer Token)
- **파일 용량 제한**: 50MB제한
- **확장자명 제한**: 이미지 (.jpg, .jpeg, .png, .gif, .webp), 문서 (.pdf, .txt, .docx, .xlsx, .pptx, .csv)

**요청 헤더**
```http
Authorization: Bearer <ACCESS_TOKEN>
Content-Type: multipart/form-data
```

**요청 본문 (multipart/form-data)**

```
curl -X POST http://localhost:8080/api/v1/files/upload \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -F "file=@/path/to/your/file.png"
```

**JavaScript/Fetch 예제**
```javascript
const formData = new FormData();
formData.append('file', fileInput.files[0]);

const response = await fetch('/api/v1/files/upload', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${accessToken}`
  },
  body: formData
});

const data = await response.json();
console.log(data.detail.fileUrl);
```

| 필드 | 타입 | 필수 | 설명 | 제약사항 |
|------|------|------|------|----------|
| file | file | ✅ | 업로드할 파일 | 최대 50MB, 이미지/문서만 허용 |

**지원 파일 타입**
- **이미지**: `.jpg`, `.jpeg`, `.png`, `.gif`, `.webp` (최대 10MB)
- **문서**: `.pdf`, `.txt`, `.docx` (최대 20MB)

**성공 응답**
- **200 OK**
  ```json
  {
    "success": true,
    "detail": {
      "fileUrl": "https://ai-hub-bucket.s3.amazonaws.com/uploads/2025/01/01/abc123def456.png",
      "fileName": "screenshot.png",
      "fileSize": 2048576,
      "contentType": "image/png",
      "uploadedAt": "2025-01-01T00:00:00Z",
      "expiresAt": "2026-01-01T00:00:00Z"
    },
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```

**응답 필드**

| 필드 | 타입 | 설명                            |
|------|------|-------------------------------|
| detail.fileUrl | string | R2에 저장된 파일의 공개 접근 URL (1년 유효) |
| detail.fileName | string | 원본 파일명                        |
| detail.fileSize | integer | 파일 크기 (바이트)                   |
| detail.contentType | string | MIME 타입                       |
| detail.uploadedAt | string | 업로드 완료 시각 (ISO 8601)          |
| detail.expiresAt | string | URL 만료 시각 (ISO 8601)          |

**오류 응답 예시**
- **400 Bad Request**: 파일 검증 실패
  ```json
  {
    "success": false,
    "detail": {
      "code": "VALIDATION_ERROR",
      "message": "지원하지 않는 파일 형식입니다.",
      "details": "contentType=application/exe"
    },
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```
- **413 Payload Too Large**: 파일 크기 초과
  ```json
  {
    "success": false,
    "detail": {
      "code": "VALIDATION_ERROR",
      "message": "파일 크기가 너무 큽니다.",
      "details": "maxSize=52428800,uploadedSize=104857600"
    },
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```
- **401 Unauthorized**: 인증 실패
  ```json
  {
    "success": false,
    "detail": {
      "code": "INVALID_TOKEN",
      "message": "인증이 필요합니다.",
      "details": null
    },
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```
- **503 Service Unavailable**: R2 업로드 실패
  ```json
  {
    "success": false,
    "detail": {
      "code": "SYSTEM_ILLEGAL_STATE",
      "message": "파일 업로드 중 오류가 발생했습니다.",
      "details": "R2 서비스 일시적 오류"
    },
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```

**사용 흐름**
1. **파일 업로드**: `POST /api/v1/files/upload`로 파일을 multipart 형식으로 업로드
2. **URL 획득**: 응답의 `detail.fileUrl`에서 R2 URL 획득
3. **메시지 전송**: 메시지 전송 API에서 `fileUrl` 필드에 획득한 URL을 사용



---

### 5. 메시지 (Message)

#### 메시지 전송 및 AI 응답
- **Method**: SSE `/api/v1/chat-rooms/{roomId}/messages`
- **설명**: 사용자가 메시지를 전송하면 AI 모델 응답과 코인 차감 결과를 반환합니다.
- **인증**: 필수 (Bearer Token)

**요청 헤더**
```http
Authorization: Bearer <ACCESS_TOKEN>
Content-Type: application/json
```

**경로 변수**

| 변수 | 타입 | 설명 |
|------|------|------|
| roomId | string | 메시지를 전송할 채팅방 UUID |

**요청 본문**
```json
{
  "content": "안녕하세요, AI!",
  "fileUrl": "https://example.com/file.png",
  "modelId": 1
}
```

**요청 필드**

| 필드 | 타입 | 필수 | 설명 | 제약사항 |
|------|------|------|------|----------|
| content | string | ✅ | 사용자 메시지 내용 | 최대 4,000자 |
| fileUrl | string | ❌ | 첨부 파일 URL | HTTPS만 허용 |
| modelId | integer | ✅ | 사용할 AI 모델 ID | 채팅방에 허용된 모델 |

**성공 응답**
- **201 Created**
  ```json
  {
    "success": true,
    "detail": {
      "userMessage": {
        "messageId": "uuid-v7",
        "roomId": "uuid-v7",
        "role": "user",
        "content": "안녕하세요, AI!",
        "fileUrl": "https://example.com/file.png",
        "tokenCount": 5.0,
        "coinCount": 0.0001,
        "modelId": 1,
        "createdAt": "2025-01-01T00:00:00Z"
      },
      "assistantMessage": {
        "messageId": "uuid-v7",
        "roomId": "uuid-v7",
        "role": "assistant",
        "content": "안녕하세요! 무엇을 도와드릴까요?",
        "tokenCount": 10.0,
        "coinCount": 0.0002,
        "modelId": 1,
        "createdAt": "2025-01-01T00:00:01Z"
      },
      "totalCoinUsed": 0.0003,
      "remainingBalance": 99.9997
    },
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```

**응답 필드**

| 필드 | 타입 | 설명 |
|------|------|------|
| detail.userMessage | object | 사용자가 전송한 메시지 정보 |
| detail.userMessage.messageId | string | 메시지 UUID |
| detail.userMessage.roomId | string | 채팅방 UUID |
| detail.userMessage.role | string | 메시지 역할 (user) |
| detail.userMessage.content | string | 메시지 내용 |
| detail.userMessage.fileUrl | string | 첨부 파일 URL (선택적, null 가능) |
| detail.userMessage.tokenCount | number | 사용된 토큰 수 |
| detail.userMessage.coinCount | number | 차감된 코인 수 |
| detail.userMessage.modelId | integer | 사용 모델 ID |
| detail.userMessage.createdAt | string | 메시지 생성 시각 |
| detail.assistantMessage | object | AI 응답 메시지 정보 |
| detail.assistantMessage.messageId | string | 메시지 UUID |
| detail.assistantMessage.roomId | string | 채팅방 UUID |
| detail.assistantMessage.role | string | 메시지 역할 (assistant) |
| detail.assistantMessage.content | string | AI 응답 내용 |
| detail.assistantMessage.fileUrl | string | AI 생성 파일 URL (선택적, null 가능) |
| detail.assistantMessage.tokenCount | number | 사용된 토큰 수 |
| detail.assistantMessage.coinCount | number | 차감된 코인 수 |
| detail.assistantMessage.modelId | integer | 사용 모델 ID |
| detail.assistantMessage.createdAt | string | 메시지 생성 시각 |
| detail.totalCoinUsed | number | 요청 처리 중 사용된 총 코인 |
| detail.remainingBalance | number | 처리 후 남은 코인 잔액 |

**오류 응답 예시**
- **400 Bad Request**: 검증 실패
  ```json
  {
    "success": false,
    "detail": {
      "code": "VALIDATION_ERROR",
      "message": "content는 비어 있을 수 없습니다.",
      "details": null
    },
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```
- **402 Payment Required**: 코인 잔액 부족
  ```json
  {
    "success": false,
    "detail": {
      "code": "INSUFFICIENT_BALANCE",
      "message": "코인 잔액이 부족합니다.",
      "details": "requiredCoin=0.5"
    },
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```
- **404 Not Found**: 채팅방 또는 모델 없음
  ```json
  {
    "success": false,
    "detail": {
      "code": "ROOM_NOT_FOUND",
      "message": "채팅방이 존재하지 않습니다.",
      "details": null
    },
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```

#### 메시지 목록 조회
- **Method**: GET `/api/v1/chat-rooms/{roomId}/messages`
- **설명**: 특정 채팅방의 메시지를 페이지네이션하여 조회합니다.
- **인증**: 필수 (Bearer Token)

**요청 헤더**
```http
Authorization: Bearer <ACCESS_TOKEN>
```

**경로 변수**

| 변수 | 타입 | 설명 |
|------|------|------|
| roomId | string | 메시지를 조회할 채팅방 UUID |

**쿼리 파라미터**

| 파라미터 | 타입 | 기본값 | 설명 | 제약사항 |
|---------|------|--------|------|----------|
| page | integer | 0 | 페이지 번호 | 0 이상 |
| size | integer | 50 | 페이지 크기 | 1~200 |
| sort | string | createdAt,asc | 정렬 조건 | `필드,방향` |

**성공 응답**
- **200 OK**
  ```json
  {
    "success": true,
    "detail": {
      "content": [
        {
          "messageId": "uuid-v7",
          "role": "user",
          "content": "메시지 내용",
          "tokenCount": 5.0,
          "coinCount": 0.0001,
          "modelId": 1,
          "createdAt": "2025-01-01T00:00:00Z"
        }
      ],
      "totalElements": 100,
      "totalPages": 2,
      "size": 50,
      "number": 0
    },
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```

**응답 필드**

| 필드 | 타입 | 설명 |
|------|------|------|
| detail.content[] | array | 메시지 배열 |
| detail.content[].messageId | string | 메시지 UUID |
| detail.content[].role | string | 메시지 역할 (user, assistant) |
| detail.content[].content | string | 메시지 내용 |
| detail.content[].tokenCount | number | 사용된 토큰 수 |
| detail.content[].coinCount | number | 차감된 코인 수 |
| detail.content[].modelId | integer | 사용 모델 ID |
| detail.content[].createdAt | string | 메시지 생성 시각 |
| detail.totalElements | integer | 전체 메시지 수 |
| detail.totalPages | integer | 전체 페이지 수 |
| detail.size | integer | 페이지 크기 |
| detail.number | integer | 현재 페이지 번호 |

**오류 응답 예시**
- **404 Not Found**: 채팅방 없음
  ```json
  {
    "success": false,
    "detail": {
      "code": "ROOM_NOT_FOUND",
      "message": "채팅방이 존재하지 않습니다.",
      "details": null
    },
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```
- **403 Forbidden**: 다른 사용자의 채팅방
  ```json
  {
    "success": false,
    "detail": {
      "code": "FORBIDDEN",
      "message": "접근 권한이 없습니다.",
      "details": null
    },
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```

#### 메시지 상세 조회
- **Method**: GET `/api/v1/messages/{messageId}`
- **설명**: 특정 메시지의 상세 정보를 조회합니다.
- **인증**: 필수 (Bearer Token)

**요청 헤더**
```http
Authorization: Bearer <ACCESS_TOKEN>
```

**경로 변수**

| 변수 | 타입 | 설명 |
|------|------|------|
| messageId | string | 조회할 메시지 UUID |

**성공 응답**
- **200 OK**
  ```json
  {
    "success": true,
    "detail": {
      "messageId": "uuid-v7",
      "roomId": "uuid-v7",
      "role": "user",
      "content": "메시지 내용",
      "fileUrl": "https://example.com/file.png",
      "tokenCount": 5.0,
      "coinCount": 0.0001,
      "modelId": 1,
      "createdAt": "2025-01-01T00:00:00Z"
    },
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```

**응답 필드**

| 필드 | 타입 | 설명 |
|------|------|------|
| detail.messageId | string | 메시지 UUID |
| detail.roomId | string | 메시지가 속한 채팅방 UUID |
| detail.role | string | 메시지 역할 (user, assistant) |
| detail.content | string | 메시지 내용 |
| detail.fileUrl | string | 첨부 파일 URL (선택적, null 가능) |
| detail.tokenCount | number | 사용된 토큰 수 |
| detail.coinCount | number | 차감된 코인 수 |
| detail.modelId | integer | 사용 모델 ID |
| detail.createdAt | string | 메시지 생성 시각 |

**오류 응답 예시**
- **404 Not Found**: 메시지를 찾을 수 없음
  ```json
  {
    "success": false,
    "detail": {
      "code": "MESSAGE_NOT_FOUND",
      "message": "메시지를 찾을 수 없습니다.",
      "details": null
    },
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```
- **403 Forbidden**: 다른 사용자의 메시지 접근
  ```json
  {
    "success": false,
    "detail": {
      "code": "FORBIDDEN",
      "message": "접근 권한이 없습니다.",
      "details": null
    },
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```

### 5. AI 모델 (AIModel)

#### AI 모델 목록 조회
- **Method**: GET `/api/v1/models`
- **설명**: 활성화된 AI 모델 목록을 조회합니다.
- **인증**: Public

**성공 응답**
- **200 OK**
  ```json
  {
    "success": true,
    "detail": [
      {
        "modelId": 1,
        "modelName": "gpt-4",
        "displayName": "GPT-4",
        "displayExplain": "OpenAI의 최신 대화형 AI 모델",
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

**응답 필드**

| 필드 | 타입 | 설명 |
|------|------|------|
| detail[].modelId | integer | 모델 ID |
| detail[].modelName | string | 내부 모델 식별자 |
| detail[].displayName | string | 사용자 표시 이름 |
| detail[].displayExplain | string | 모델 설명 |
| detail[].inputPricePer1m | number | 입력 1m 토큰당 가격 |
| detail[].outputPricePer1m | number | 출력 1m 토큰당 가격 |
| detail[].isActive | boolean | 활성화 여부 |
| detail[].createdAt | string | 생성 시각 (ISO 8601) |
| detail[].updatedAt | string | 수정 시각 (ISO 8601) |

**오류 응답 예시**
- **503 Service Unavailable**: 가격 데이터 동기화 실패
  ```json
  {
    "success": false,
    "detail": {
      "code": "SYSTEM_ILLEGAL_STATE",
      "message": "모델 가격 정보를 가져오지 못했습니다.",
      "details": null
    },
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```

#### AI 모델 상세 조회
- **Method**: GET `/api/v1/models/{modelId}`
- **설명**: 특정 AI 모델의 상세 정보를 조회합니다.
- **인증**: Public

**경로 변수**

| 변수 | 타입 | 설명 |
|------|------|------|
| modelId | integer | 조회할 모델 ID |

**성공 응답**
- **200 OK**
  ```json
  {
    "success": true,
    "detail": {
      "modelId": 1,
      "modelName": "gpt-4",
      "displayName": "GPT-4",
      "displayExplain": "OpenAI의 최신 대화형 AI 모델",
      "inputPricePer1m": 0.03,
      "outputPricePer1m": 0.06,
      "isActive": true,
      "createdAt": "2025-01-01T00:00:00Z",
      "updatedAt": "2025-01-01T00:00:00Z"
    },
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```

**응답 필드**

| 필드 | 타입 | 설명 |
|------|------|------|
| detail.modelId | integer | 모델 ID |
| detail.modelName | string | 내부 모델 식별자 |
| detail.displayName | string | 사용자 표시 이름 |
| detail.displayExplain | string | 모델 설명 |
| detail.inputPricePer1m | number | 입력 1m 토큰당 가격 |
| detail.outputPricePer1m | number | 출력 1m 토큰당 가격 |
| detail.isActive | boolean | 활성화 여부 |
| detail.createdAt | string | 생성 시각 (ISO 8601) |
| detail.updatedAt | string | 수정 시각 (ISO 8601) |

**오류 응답 예시**
- **404 Not Found**: 모델 없음
  ```json
  {
    "success": false,
    "detail": {
      "code": "MODEL_NOT_FOUND",
      "message": "요청한 모델을 찾을 수 없습니다.",
      "details": null
    },
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```

#### [관리자] AI 모델 등록
- **Method**: POST `/api/v1/admin/models`
- **설명**: 새로운 AI 모델을 등록합니다.
- **인증**: 필수 (관리자 권한)

**요청 헤더**
```http
Authorization: Bearer <ACCESS_TOKEN>
Content-Type: application/json
```

**요청 본문**
```json
{
  "modelName": "gpt-4-turbo",
  "displayName": "GPT-4 Turbo",
  "displayExplain": "더 빠르고 저렴한 GPT-4",
  "inputPricePer1m": 0.01,
  "outputPricePer1m": 0.03,
  "isActive": true
}
```

**요청 필드**

| 필드 | 타입 | 필수 | 설명 | 제약사항 |
|------|------|------|------|----------|
| modelName | string | ✅ | 시스템 내부 모델 식별자 | 소문자, 하이픈 허용 |
| displayName | string | ✅ | 프런트 표시 이름 | 최대 30자 |
| displayExplain | string | ❌ | 모델 설명 | 최대 200자 |
| inputPricePer1m | number | ✅ | 입력 1m 토큰당 USD 가격 | 0 이상 |
| outputPricePer1m | number | ✅ | 출력 1m 토큰당 USD 가격 | 0 이상 |
| isActive | boolean | ✅ | 활성화 여부 | - |

**성공 응답**
- **201 Created**
  ```json
  {
    "success": true,
    "detail": {
      "modelId": 3,
      "modelName": "gpt-4-turbo",
      "displayName": "GPT-4 Turbo",
      "displayExplain": "더 빠르고 저렴한 GPT-4",
      "inputPricePer1m": 0.01,
      "outputPricePer1m": 0.03,
      "isActive": true,
      "createdAt": "2025-01-01T00:00:00Z",
      "updatedAt": "2025-01-01T00:00:00Z"
    },
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```

**응답 필드**: `AI 모델 상세 조회`와 동일

**오류 응답 예시**
- **400 Bad Request**: 검증 실패
  ```json
  {
    "success": false,
    "detail": {
      "code": "VALIDATION_ERROR",
      "message": "modelName은 고유해야 합니다.",
      "details": null
    },
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```
- **403 Forbidden**: 권한 부족
  ```json
  {
    "success": false,
    "detail": {
      "code": "FORBIDDEN",
      "message": "관리자 권한이 필요합니다.",
      "details": null
    },
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```

#### [관리자] AI 모델 수정
- **Method**: PUT `/api/v1/admin/models/{modelId}`
- **설명**: AI 모델 정보를 수정합니다.
- **인증**: 필수 (관리자 권한)

**요청 헤더**
```http
Authorization: Bearer <ACCESS_TOKEN>
Content-Type: application/json
```

**경로 변수**

| 변수 | 타입 | 설명 |
|------|------|------|
| modelId | integer | 수정할 모델 ID |

**요청 본문**
```json
{
  "displayName": "GPT-4 Turbo",
  "displayExplain": "업데이트된 설명",
  "inputPricePer1m": 0.01,
  "outputPricePer1m": 0.03,
  "isActive": true
}
```

**요청 필드**

| 필드 | 타입 | 필수 | 설명 | 제약사항 |
|------|------|------|------|----------|
| displayName | string | ❌ | 사용자 표시 이름 | 최대 30자 |
| displayExplain | string | ❌ | 모델 설명 | 최대 200자 |
| inputPricePer1m | number | ❌ | 입력 1m 토큰당 가격 | 0 이상 |
| outputPricePer1m | number | ❌ | 출력 1m 토큰당 가격 | 0 이상 |
| isActive | boolean | ❌ | 활성화 여부 | - |

**성공 응답**
- **200 OK**
  ```json
  {
    "success": true,
    "detail": {
      "modelId": 1,
      "modelName": "gpt-4",
      "displayName": "GPT-4 Turbo",
      "displayExplain": "업데이트된 설명",
      "inputPricePer1m": 0.01,
      "outputPricePer1m": 0.03,
      "isActive": true,
      "createdAt": "2024-12-01T00:00:00Z",
      "updatedAt": "2025-01-02T00:00:00Z"
    },
    "timestamp": "2025-01-02T00:00:00Z"
  }
  ```

**응답 필드**: `AI 모델 상세 조회`와 동일

**오류 응답 예시**
- **404 Not Found**: 모델 없음
  ```json
  {
    "success": false,
    "detail": {
      "code": "MODEL_NOT_FOUND",
      "message": "요청한 모델을 찾을 수 없습니다.",
      "details": null
    },
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```
- **400 Bad Request**: 가격이 음수
  ```json
  {
    "success": false,
    "detail": {
      "code": "VALIDATION_ERROR",
      "message": "가격은 0 이상이어야 합니다.",
      "details": "inputPricePer1m=-0.1"
    },
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```

#### [관리자] AI 모델 삭제
- **Method**: DELETE `/api/v1/admin/models/{modelId}`
- **설명**: AI 모델을 삭제합니다.
- **인증**: 필수 (관리자 권한)

**요청 헤더**
```http
Authorization: Bearer <ACCESS_TOKEN>
```

**경로 변수**

| 변수 | 타입 | 설명 |
|------|------|------|
| modelId | integer | 삭제할 모델 ID |

**성공 응답**
- **204 No Content**

**오류 응답 예시**
- **404 Not Found**: 모델 없음
  ```json
  {
    "success": false,
    "detail": {
      "code": "MODEL_NOT_FOUND",
      "message": "요청한 모델을 찾을 수 없습니다.",
      "details": null
    },
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```
- **409 Conflict**: 이미 참조 중인 모델
  ```json
  {
    "success": false,
    "detail": {
      "code": "CONFLICT",
      "message": "활성 채팅방에서 사용 중인 모델입니다.",
      "details": null
    },
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```

### 6. 지갑 (UserWallet)

#### 지갑 조회
- **Method**: GET `/api/v1/wallet`
- **설명**: 현재 사용자의 지갑 상세 정보를 조회합니다.
- **인증**: 필수 (Bearer Token)

**요청 헤더**
```http
Authorization: Bearer <ACCESS_TOKEN>
```

**성공 응답**
- **200 OK**
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

**응답 필드**

| 필드 | 타입 | 설명 |
|------|------|------|
| detail.walletId | integer | 지갑 ID |
| detail.userId | integer | 사용자 ID |
| detail.balance | number | 현재 코인 잔액 |
| detail.totalPurchased | number | 누적 충전 코인 |
| detail.totalUsed | number | 누적 사용 코인 |
| detail.lastTransactionAt | string | 마지막 거래 시각 |
| detail.createdAt | string | 지갑 생성 시각 |
| detail.updatedAt | string | 지갑 수정 시각 |

**오류 응답 예시**
- **401 Unauthorized**: 인증 실패
  ```json
  {
    "success": false,
    "detail": {
      "code": "INVALID_TOKEN",
      "message": "인증이 필요합니다.",
      "details": null
    },
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```
- **404 Not Found**: 지갑 미생성
  ```json
  {
    "success": false,
    "detail": {
      "code": "WALLET_NOT_FOUND",
      "message": "지갑 정보가 존재하지 않습니다.",
      "details": null
    },
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```

#### 잔액 조회
- **Method**: GET `/api/v1/wallet/balance`
- **설명**: 현재 사용자의 코인 잔액만 조회합니다.
- **인증**: 필수 (Bearer Token)

**요청 헤더**
```http
Authorization: Bearer <ACCESS_TOKEN>
```

**성공 응답**
- **200 OK**
  ```json
  {
    "success": true,
    "detail": {
      "balance": 100.5
    },
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```

**응답 필드**

| 필드 | 타입 | 설명 |
|------|------|------|
| detail.balance | number | 현재 코인 잔액 |

**오류 응답 예시**
- **401 Unauthorized**: 인증 실패
  ```json
  {
    "success": false,
    "detail": {
      "code": "INVALID_TOKEN",
      "message": "인증이 필요합니다.",
      "details": null
    },
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```
- **404 Not Found**: 지갑 미생성
  ```json
  {
    "success": false,
    "detail": {
      "code": "WALLET_NOT_FOUND",
      "message": "지갑 정보가 존재하지 않습니다.",
      "details": null
    },
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```

### 7. 결제 (PaymentHistory)

#### 코인 충전 요청
- **Method**: POST `/api/v1/payments`
- **설명**: 결제 게이트웨이에 코인 충전 요청을 생성합니다.
- **인증**: 필수 (Bearer Token)

**요청 헤더**
```http
Authorization: Bearer <ACCESS_TOKEN>
Content-Type: application/json
```

**요청 본문**
```json
{
  "amountKrw": 10000.0,
  "paymentMethod": "card",
  "paymentGateway": "toss"
}
```

**요청 필드**

| 필드 | 타입 | 필수 | 설명 | 제약사항 |
|------|------|------|------|----------|
| amountKrw | number | ✅ | 결제 금액 (KRW) | 1,000 이상 |
| paymentMethod | string | ✅ | 결제 수단 | `card`, `transfer` 등 |
| paymentGateway | string | ✅ | 결제 게이트웨이 코드 | 지원되는 게이트웨이만 허용 |

**성공 응답**
- **201 Created**
  ```json
  {
    "success": true,
    "detail": {
      "paymentId": 1,
      "transactionId": "unique-transaction-id",
      "amountKrw": 10000.0,
      "amountUsd": 7.5,
      "coinAmount": 7.5,
      "bonusCoin": 0.5,
      "status": "pending",
      "paymentUrl": "https://payment-gateway.com/checkout/abc123",
      "createdAt": "2025-01-01T00:00:00Z"
    },
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```

**응답 필드**

| 필드 | 타입 | 설명 |
|------|------|------|
| detail.paymentId | integer | 생성된 결제 ID |
| detail.transactionId | string | 게이트웨이 거래 ID |
| detail.amountKrw | number | 결제 금액 (KRW) |
| detail.amountUsd | number | 환산된 USD 금액 |
| detail.coinAmount | number | 지급 예정 코인 |
| detail.bonusCoin | number | 지급 예정 보너스 코인 |
| detail.status | string | 현재 결제 상태 |
| detail.paymentUrl | string | 결제 진행 URL |
| detail.createdAt | string | 요청 생성 시각 |

**오류 응답 예시**
- **400 Bad Request**: 결제 금액 검증 실패
  ```json
  {
    "success": false,
    "detail": {
      "code": "VALIDATION_ERROR",
      "message": "amountKrw는 1000 이상이어야 합니다.",
      "details": null
    },
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```
- **409 Conflict**: 중복 결제 요청
  ```json
  {
    "success": false,
    "detail": {
      "code": "CONFLICT",
      "message": "이미 처리 중인 결제 요청이 있습니다.",
      "details": "transactionId=unique-transaction-id"
    },
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```

#### 결제 내역 조회
- **Method**: GET `/api/v1/payments`
- **설명**: 현재 사용자의 결제 내역을 페이지네이션하여 조회합니다.
- **인증**: 필수 (Bearer Token)

**요청 헤더**
```http
Authorization: Bearer <ACCESS_TOKEN>
```

**쿼리 파라미터**

| 파라미터 | 타입 | 기본값 | 설명 | 제약사항 |
|---------|------|--------|------|----------|
| page | integer | 0 | 페이지 번호 | 0 이상 |
| size | integer | 20 | 페이지 크기 | 1~100 |
| status | string | - | 결제 상태 필터 | `pending`, `completed`, `failed`, `cancelled` |

**성공 응답**
- **200 OK**
  ```json
  {
    "success": true,
    "detail": {
      "content": [
        {
          "paymentId": 1,
          "transactionId": "unique-transaction-id",
          "paymentMethod": "card",
          "amountKrw": 10000.0,
          "coinAmount": 7.5,
          "bonusCoin": 0.5,
          "status": "completed",
          "createdAt": "2025-01-01T00:00:00Z",
          "completedAt": "2025-01-01T00:01:00Z"
        }
      ],
      "totalElements": 50,
      "totalPages": 3,
      "size": 20,
      "number": 0
    },
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```

**오류 응답 예시**
- **400 Bad Request**: 잘못된 상태값
  ```json
  {
    "success": false,
    "detail": {
      "code": "VALIDATION_ERROR",
      "message": "지원하지 않는 결제 상태입니다.",
      "details": "status=unknown"
    },
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```
- **401 Unauthorized**: 인증 실패
  ```json
  {
    "success": false,
    "detail": {
      "code": "INVALID_TOKEN",
      "message": "인증이 필요합니다.",
      "details": null
    },
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```

#### 결제 상세 조회
- **Method**: GET `/api/v1/payments/{paymentId}`
- **설명**: 특정 결제 건의 상세 정보를 조회합니다.
- **인증**: 필수 (Bearer Token)

**요청 헤더**
```http
Authorization: Bearer <ACCESS_TOKEN>
```

**경로 변수**

| 변수 | 타입 | 설명 |
|------|------|------|
| paymentId | integer | 조회할 결제 ID |

**성공 응답**
- **200 OK**
  ```json
  {
    "success": true,
    "detail": {
      "paymentId": 1,
      "transactionId": "unique-transaction-id",
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

**오류 응답 예시**
- **404 Not Found**: 결제 기록 없음
  ```json
  {
    "success": false,
    "detail": {
      "code": "PAYMENT_NOT_FOUND",
      "message": "결제 내역을 찾을 수 없습니다.",
      "details": null
    },
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```
- **403 Forbidden**: 다른 사용자의 결제 접근
  ```json
  {
    "success": false,
    "detail": {
      "code": "FORBIDDEN",
      "message": "접근 권한이 없습니다.",
      "details": null
    },
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```

#### 결제 완료 웹훅
- **Method**: POST `/api/v1/payments/webhook`
- **설명**: 결제 게이트웨이에서 전달한 웹훅을 처리하여 결제 완료/실패 후속 작업을 수행합니다.
- **인증**: 게이트웨이별 시그니처 검증

**요청 헤더**
| 헤더 | 필수 | 설명 |
|------|------|------|
| Toss-Signature | ✅ | Toss 등 PG 사 서명값. 시그니처 검증에 사용합니다. |
| Content-Type | ✅ | `application/json` |
| X-Webhook-Id | ✅ | 게이트웨이가 부여한 웹훅 이벤트 고유 ID |

**요청 본문**
```json
{
  "eventType": "PAYMENT_APPROVED",
  "eventId": "evt_20250101_0001",
  "occurredAt": "2025-01-01T09:00:00+09:00",
  "data": {
    "orderId": "order-uuid",
    "paymentKey": "pay_1234567890",
    "transactionId": "unique-transaction-id",
    "status": "DONE",
    "totalAmount": 33000,
    "currency": "KRW",
    "approvedAt": "2025-01-01T09:00:00+09:00",
    "method": "CARD",
    "customer": {
      "customerId": 1,
      "email": "user@example.com"
    },
    "metadata": {
      "paymentPlanId": 3,
      "note": "정기 결제"
    }
  }
}
```

**요청 필드**
| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| eventType | string | ✅ | PG에서 발행한 이벤트 타입 (`PAYMENT_APPROVED`, `PAYMENT_FAILED` 등) |
| eventId | string | ✅ | 웹훅 이벤트 고유 ID (중복 처리 방지) |
| occurredAt | string | ✅ | 이벤트 발생 시각 (ISO 8601) |
| data.orderId | string | ✅ | 내부 주문 번호 (결제 생성 시 전달한 값) |
| data.paymentKey | string | ✅ | 게이트웨이 결제 키 |
| data.transactionId | string | ✅ | PG 결제 트랜잭션 ID |
| data.status | string | ✅ | 결제 상태 (`DONE`, `CANCELED`, `FAILED`) |
| data.totalAmount | integer | ✅ | 결제 총 금액 (KRW) |
| data.currency | string | ✅ | 통화 코드 (`KRW`, `USD` 등) |
| data.approvedAt | string | ✅ | 결제 완료 시각 (ISO 8601) |
| data.method | string | ✅ | 결제 수단 (`CARD`, `VIRTUAL_ACCOUNT` 등) |
| data.customer.customerId | integer | ✅ | 사용자 ID (내부 식별자) |
| data.customer.email | string | ✅ | 결제를 진행한 고객 이메일 |
| data.metadata.paymentPlanId | integer | ❌ | 결제 플랜 ID (존재 시 플랜 결제 처리) |
| data.metadata.note | string | ❌ | 기타 메타데이터 |

**성공 응답**
- **200 OK**
  ```json
  {
    "success": true,
    "detail": {
      "status": "processed",
      "transactionId": "unique-transaction-id",
      "processedAt": "2025-01-01T00:00:00Z"
    },
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```

**응답 필드**
| 필드 | 타입 | 설명 |
|------|------|------|
| detail.status | string | 처리 결과 (`processed`, `ignored`) |
| detail.transactionId | string | 내부에서 기록한 결제 트랜잭션 ID |
| detail.processedAt | string | 서버에서 웹훅을 처리한 시각 |

**오류 응답 예시**
- **400 Bad Request**: 시그니처 검증 실패
  ```json
  {
    "success": false,
    "detail": {
      "code": "VALIDATION_ERROR",
      "message": "웹훅 시그니처가 유효하지 않습니다.",
      "details": null
    },
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```
- **401 Unauthorized**: 유효하지 않은 시그니처 헤더
  ```json
  {
    "success": false,
    "detail": {
      "code": "AUTHENTICATION_FAILED",
      "message": "웹훅 접근이 승인되지 않았습니다.",
      "details": "signature header missing"
    },
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```
- **409 Conflict**: 이미 처리된 트랜잭션
  ```json
  {
    "success": false,
    "detail": {
      "code": "CONFLICT",
      "message": "이미 처리된 결제입니다.",
      "details": "transactionId=unique-transaction-id"
    },
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```

#### 결제 취소
- **Method**: POST `/api/v1/payments/{paymentId}/cancel`
- **설명**: 완료된 결제를 취소하고 코인을 회수합니다.
- **인증**: 필수 (Bearer Token)

**요청 헤더**
```http
Authorization: Bearer <ACCESS_TOKEN>
Content-Type: application/json
```

**경로 변수**

| 변수 | 타입 | 설명 |
|------|------|------|
| paymentId | integer | 취소할 결제 ID |

**요청 본문**
```json
{
  "reason": "고객 변심"
}
```

**요청 필드**

| 필드 | 타입 | 필수 | 설명 | 제약사항 |
|------|------|------|------|----------|
| reason | string | ✅ | 취소 사유 | 최대 200자 |

**성공 응답**
- **200 OK**
  ```json
  {
    "success": true,
    "detail": {
      "paymentId": 1,
      "transactionId": "unique-transaction-id",
      "status": "cancelled",
      "amountKrw": 10000.0,
      "coinAmount": 7.5,
      "refundedCoin": 7.5,
      "cancelledAt": "2025-01-01T00:05:00Z"
    },
    "timestamp": "2025-01-01T00:05:00Z"
  }
  ```

**응답 필드**

| 필드 | 타입 | 설명 |
|------|------|------|
| detail.paymentId | integer | 취소된 결제 ID |
| detail.transactionId | string | 게이트웨이 거래 ID |
| detail.status | string | 결제 상태 (`cancelled`) |
| detail.amountKrw | number | 결제 금액 |
| detail.coinAmount | number | 지급된 코인 |
| detail.refundedCoin | number | 환불된 코인 |
| detail.cancelledAt | string | 결제 취소 시각 |

**오류 응답 예시**
- **404 Not Found**: 결제 기록 없음
  ```json
  {
    "success": false,
    "detail": {
      "code": "PAYMENT_NOT_FOUND",
      "message": "결제 내역을 찾을 수 없습니다.",
      "details": null
    },
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```
- **409 Conflict**: 취소 불가 상태
  ```json
  {
    "success": false,
    "detail": {
      "code": "CONFLICT",
      "message": "현재 상태에서 결제를 취소할 수 없습니다.",
      "details": "status=failed"
    },
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```

### 8. 코인 거래 내역 (CoinTransaction)

#### 코인 거래 내역 조회
- **Method**: GET `/api/v1/transactions`
- **설명**: 현재 사용자의 코인 거래 내역을 필터링하여 조회합니다.
- **인증**: 필수 (Bearer Token)

**요청 헤더**
```http
Authorization: Bearer <ACCESS_TOKEN>
```

**쿼리 파라미터**

| 파라미터 | 타입 | 기본값 | 설명 | 제약사항 |
|---------|------|--------|------|----------|
| page | integer | 0 | 페이지 번호 | 0 이상 |
| size | integer | 20 | 페이지 크기 | 1~100 |
| transactionType | string | - | 거래 유형 필터 | `purchase`, `usage`, `refund`, `bonus` |
| startDate | string | - | ISO 날짜(YYYY-MM-DD) | 종료 일자와 함께 사용 |
| endDate | string | - | ISO 날짜(YYYY-MM-DD) | 시작 일자 이상 |

**성공 응답**
- **200 OK**
  ```json
  {
    "success": true,
    "detail": {
      "content": [
        {
          "transactionId": 1,
          "transactionType": "usage",
          "amount": -0.0003,
          "balanceAfter": 99.9997,
          "description": "GPT-4 메시지 전송",
          "modelId": 1,
          "modelName": "gpt-4",
          "roomId": "uuid-v7",
          "messageId": "uuid-v7",
          "createdAt": "2025-01-01T00:00:00Z"
        }
      ],
      "totalElements": 500,
      "totalPages": 25,
      "size": 20,
      "number": 0
    },
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```

**응답 필드**
| 필드 | 타입 | 설명 |
|------|------|------|
| detail.content | array | 페이지네이션된 거래 목록 |
| detail.content[].transactionId | integer | 거래 ID |
| detail.content[].transactionType | string | 거래 유형 (`purchase`, `usage`, `refund`, `bonus`) |
| detail.content[].amount | number | 거래 금액 (코인 증감, 음수는 차감) |
| detail.content[].balanceAfter | number | 거래 직후 잔액 |
| detail.content[].description | string | 거래에 대한 설명 |
| detail.content[].modelId | integer | 연관된 모델 ID (usage 유형에서 사용) |
| detail.content[].modelName | string | 연관된 모델 시스템 이름 |
| detail.content[].roomId | string | 연관된 채팅방 UUID (usage 유형에서 사용) |
| detail.content[].messageId | string | 연관된 메시지 UUID (usage 유형에서 사용) |
| detail.content[].createdAt | string | 거래 생성 시각 (ISO 8601) |
| detail.totalElements | integer | 전체 거래 건수 |
| detail.totalPages | integer | 전체 페이지 수 |
| detail.size | integer | 페이지당 항목 수 |
| detail.number | integer | 현재 페이지 번호 (0 기반) |

**오류 응답 예시**
- **400 Bad Request**: 날짜 범위 오류
  ```json
  {
    "success": false,
    "detail": {
      "code": "VALIDATION_ERROR",
      "message": "종료 일자는 시작 일자 이후여야 합니다.",
      "details": "startDate=2025-02-01,endDate=2025-01-01"
    },
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```
- **401 Unauthorized**: 인증 실패
  ```json
  {
    "success": false,
    "detail": {
      "code": "INVALID_TOKEN",
      "message": "인증이 필요합니다.",
      "details": null
    },
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```

#### 거래 상세 조회
- **Method**: GET `/api/v1/transactions/{transactionId}`
- **설명**: 특정 코인 거래의 상세 정보를 조회합니다.
- **인증**: 필수 (Bearer Token)

**요청 헤더**
```http
Authorization: Bearer <ACCESS_TOKEN>
```

**경로 변수**

| 변수 | 타입 | 설명 |
|------|------|------|
| transactionId | integer | 조회할 거래 ID |

**성공 응답**
- **200 OK**
  ```json
  {
    "success": true,
    "detail": {
      "transactionId": 1,
      "userId": 1,
      "roomId": "uuid-v7",
      "messageId": "uuid-v7",
      "transactionType": "usage",
      "amount": -0.0003,
      "balanceAfter": 99.9997,
      "description": "GPT-4 메시지 전송",
      "modelId": 1,
      "createdAt": "2025-01-01T00:00:00Z"
    },
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```

**응답 필드**
| 필드 | 타입 | 설명 |
|------|------|------|
| detail.transactionId | integer | 거래 ID |
| detail.userId | integer | 거래 소유자 ID |
| detail.roomId | string | 거래와 연관된 채팅방 UUID (usage 유형에서만 존재) |
| detail.messageId | string | 거래와 연관된 메시지 UUID (usage 유형에서만 존재) |
| detail.transactionType | string | 거래 유형 (`purchase`, `usage`, `refund`, `bonus`) |
| detail.amount | number | 거래 금액 (코인 증감) |
| detail.balanceAfter | number | 거래 직후 잔액 |
| detail.description | string | 거래 설명 |
| detail.modelId | integer | 연관된 모델 ID (usage 유형에서만 존재) |
| detail.createdAt | string | 거래 생성 시각 (ISO 8601) |

**오류 응답 예시**
- **404 Not Found**: 거래 없음
  ```json
  {
    "success": false,
    "detail": {
      "code": "TRANSACTION_NOT_FOUND",
      "message": "거래 내역을 찾을 수 없습니다.",
      "details": null
    },
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```
- **403 Forbidden**: 다른 사용자의 거래 접근
  ```json
  {
    "success": false,
    "detail": {
      "code": "FORBIDDEN",
      "message": "접근 권한이 없습니다.",
      "details": null
    },
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```

### 9. 대시보드 (Dashboard)

#### 모델별 가격 대시보드
- **Method**: GET `/api/v1/dashboard/models/pricing`
- **설명**: 모든 활성화된 AI 모델의 1,000,000토큰당 USD 가격 정보를 조회합니다.
- **인증**: Public

**성공 응답**
- **200 OK**
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

**오류 응답 예시**
- **503 Service Unavailable**: 가격 계산 실패
  ```json
  {
    "success": false,
    "detail": {
      "code": "SYSTEM_ILLEGAL_STATE",
      "message": "대시보드 데이터를 불러오지 못했습니다.",
      "details": null
    },
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```

#### 월별 모델별 코인 사용량 대시보드
- **Method**: GET `/api/v1/dashboard/usage/monthly`
- **설명**: 현재 사용자의 월별 모델별 코인 사용량 통계를 조회합니다.
- **인증**: 필수 (Bearer Token)

**요청 헤더**
```http
Authorization: Bearer <ACCESS_TOKEN>
```

**쿼리 파라미터**

| 파라미터 | 타입 | 기본값 | 설명 |
|---------|------|--------|------|
| year | integer | 현재 연도 | 조회할 연도 |
| month | integer | 현재 월 | 1~12 |

**성공 응답**
- **200 OK**
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
          "tokenCount": 50000,
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

**오류 응답 예시**
- **400 Bad Request**: 잘못된 월 값
  ```json
  {
    "success": false,
    "detail": {
      "code": "VALIDATION_ERROR",
      "message": "month는 1과 12 사이여야 합니다.",
      "details": "month=13"
    },
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```
- **401 Unauthorized**: 인증 실패
  ```json
  {
    "success": false,
    "detail": {
      "code": "INVALID_TOKEN",
      "message": "인증이 필요합니다.",
      "details": null
    },
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```

#### 사용자 통계 요약
- **Method**: GET `/api/v1/dashboard/stats`
- **설명**: 현재 사용자의 코인 및 활동 통계를 요약합니다.
- **인증**: 필수 (Bearer Token)

**요청 헤더**
```http
Authorization: Bearer <ACCESS_TOKEN>
```

**성공 응답**
- **200 OK**
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

**오류 응답 예시**
- **401 Unauthorized**: 인증 실패
  ```json
  {
    "success": false,
    "detail": {
      "code": "INVALID_TOKEN",
      "message": "인증이 필요합니다.",
      "details": null
    },
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```
- **503 Service Unavailable**: 통계 집계 실패
  ```json
  {
    "success": false,
    "detail": {
      "code": "SYSTEM_ILLEGAL_STATE",
      "message": "통계 데이터를 계산할 수 없습니다.",
      "details": null
    },
    "timestamp": "2025-01-01T00:00:00Z"
  }
  ```
