# MSA AI 서버 연동

> ✅ 최신 스펙: `### (NEW) MSA AI 서버 최신 내용(아래)` 이하를 기준으로 합니다.  
> (NEW 섹션 이전의 내용은 레거시/참고용이며, 현재 AI-HUB-BE 코드와 불일치할 수 있습니다.)

## AI-HUB-BE 연동 (현재 코드 기준)

### Base URL
- `AI_SERVER_URL` (Spring 설정: `ai-server.url`)

### 1) 파일 업로드
- `POST /ai/upload?model={modelName}`
- Content-Type: `multipart/form-data`
- Form field: `file`

### 2) 채팅 (SSE 스트리밍)
- `POST /ai/chat`
- Headers: `Content-Type: application/json`, `Accept: text/event-stream`
- Request body 예시:
```json
{
  "message": "안녕하세요",
  "model": "gpt-5-mini",
  "files": [{"id":"file-6790eafb85d4290001fe6e92","type":"image"}],
  "history": [{"role":"user","content":"이 이미지를 분석해줘"}],
  "conversationId": "resp_abc123"
}
```
- 파일 타입: `image`, `document`, `audio`
- 스트리밍 이벤트(예): `{"type":"response","data":"텍스트 조각"}`, `{"type":"usage","data":{...}}`

---

### 모델 기반 제공자 선택

- API 호출 시 `model` 파라미터로 사용할 AI 제공자가 자동으로 결정됨
- 예: `model=gpt-5-mini` → OpenAI 사용
- 예: `model=claude-3-5-sonnet` → Claude 사용
- `model` 미지정 시 → `AI_PROVIDER` 환경변수의 기본값 사용

---

## 응답 형식

### 성공 응답 (2xx)

```json
{
  "success": true,
  "data": {
    "id": "resp_xyz123",
    "content": "응답 내용",
    "model": "gpt-5-mini",
    "provider": "openai",
    "usage": {
      "input_tokens": 10,
      "output_tokens": 15,
      "total_tokens": 25
    },
    "timestamp": "2024-11-16T12:34:56.789Z"
  },
  "metadata": {
    "provider": "openai",
    "timestamp": "2024-11-16T12:34:56.789Z"
  }
}
```

### 실패 응답 (4xx, 5xx)

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "ERROR_CODE",
    "message": "에러 메시지"
  }
}
```

### 응답 필드 설명

| 필드 | 타입 | 설명 |
|------|------|------|
| `success` | boolean | 요청 성공 여부 |
| `data` | object | 응답 데이터 (실패 시 null) |
| `error` | object | 에러 정보 (실패 시만 존재) |
| `metadata` | object | 메타데이터 (제공자, 타임스탐프) |

---

## API 엔드포인트

### 1️⃣ 파일 업로드

이미지 파일을 업로드하고 파일 ID를 받습니다. 이 ID는 이후 채팅 요청에서 이미지 분석에 사용됩니다.

#### 요청

```
POST /ai/upload
```

#### 쿼리 파라미터

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `model` | string | ❌ | 업로드할 제공자 결정 (기본값: AI_PROVIDER) |

#### Request Headers

```
Content-Type: multipart/form-data
```

#### Request Body

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `file` | File | ✅ | 이미지 파일 (jpg, jpeg, png, webp만 지원) |

#### 성공 응답 (201 Created)

```json
{
  "success": true,
  "data": {
    "file_id": "file-6790eafb85d4290001fe6e92"
  },
  "metadata": {
    "provider": "openai",
    "timestamp": "2024-11-16T12:34:56.789Z"
  }
}
```

#### 실패 응답 (400 Bad Request)

**경우 1: 파일이 제공되지 않음**
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "MISSING_FILE",
    "message": "파일이 제공되지 않았습니다"
  }
}
```

**경우 2: 지원하지 않는 파일 형식**
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "UNSUPPORTED_FORMAT",
    "message": "지원하지 않는 형식: pdf. 지원되는 형식: jpg, jpeg, png, webp"
  }
}
```

#### cURL 예제

```bash
curl -X POST http://localhost:3000/ai/upload?model=gpt-5-mini \
  -F "file=@image.jpg"
```

#### JavaScript 예제

```javascript
const formData = new FormData();
formData.append('file', fileInput.files[0]);

const response = await fetch(
  'http://localhost:3000/ai/upload?model=gpt-5-mini',
  {
    method: 'POST',
    body: formData
  }
);

const result = await response.json();
console.log(result.data.file_id);
```

---

### 2️⃣ 채팅 (SSE 스트리밍)

사용자 메시지를 전송하고 AI 응답을 실시간으로 스트리밍받습니다. 이미지 분석과 이전 응답 참조도 지원합니다.

#### 요청

```
POST /ai/chat
```

#### Request Headers

```
Content-Type: application/json
```

#### Request Body

```json
{
  "message": "이 이미지를 분석해주세요",
  "model": "gpt-5-mini",
  "file_id": "file-6790eafb85d4290001fe6e92",
  "previous_response_id": "resp_123abc"
}
```

#### Request Body 필드

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `message` | string | ✅ | 사용자 메시지 |
| `model` | string | ❌ | 사용할 모델 (기본값: AI_PROVIDER의 기본 모델) |
| `file_id` | string | ❌ | 업로드된 이미지 파일 ID |
| `previous_response_id` | string | ❌ | 이전 응답 ID (대화 이어가기) |

> 💡 모델 정보는 쿼리 파라미터가 아니라 JSON Body의 `model` 필드로 전달됩니다.

#### 성공 응답 (200 OK)

응답은 SSE (Server-Sent Events) 스트림으로 전달됩니다. 각 이벤트는 `data:` 프리픽스로 시작합니다.

**스트림 이벤트 예제:**

```
{"type":"response.created","response":{"id":"resp_xyz123"}}

{"type":"response.output_text.delta","delta":"안녕하세요","sequence_number":1}

{"type":"response.output_text.delta","delta":"!","sequence_number":2}

{"type":"response.completed","sequence_number":3,"response":{"id":"resp_xyz123","model":"gpt-5-mini","content":"안녕하세요!","usage":{"input_tokens":10,"output_tokens":5,"total_tokens":15}}}
```

#### 스트림 이벤트 타입

| 타입 | 설명 | 페이로드 |
|------|------|----------|
| `response.created` | 응답 생성 시작 | `response` 객체 |
| `response.output_text.delta` | 텍스트 증분 추가 | `delta` (문자열), `sequence_number` |
| `response.completed` | 응답 완료 | 전체 `response` 객체 |

#### 실패 응답 (SSE 에러 이벤트)

```
data: {"type":"error","error":{"code":"CHAT_ERROR","message":"Invalid model: invalid-model"}}
```

#### cURL 예제

```bash
curl -X POST http://localhost:3000/ai/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Hello, how are you?",
    "model": "gpt-5-mini"
  }' \
  -N  # -N 플래그로 스트리밍 활성화
```

#### JavaScript 예제 (fetch)

```javascript
const response = await fetch('http://localhost:3000/ai/chat', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    message: 'Hello, how are you?',
    model: 'gpt-5-mini'
  })
});

const reader = response.body.getReader();
const decoder = new TextDecoder();

while (true) {
  const { done, value } = await reader.read();
  if (done) break;

  const text = decoder.decode(value);
  const lines = text.split('\n');

  for (const line of lines) {
    if (line.startsWith('data: ')) {
      const event = JSON.parse(line.slice(6));

      if (event.type === 'response.output_text.delta') {
        console.log(event.delta);  // 증분 텍스트 출력
      } else if (event.type === 'response.completed') {
        console.log('Response ID:', event.response.id);
        console.log('Tokens used:', event.response.usage.total_tokens);
      }
    }
  }
}
```

#### JavaScript 예제 (이미지 포함)

```javascript
// 1. 먼저 이미지 업로드
const uploadResponse = await fetch(
  'http://localhost:3000/ai/upload?model=gpt-5-mini',
  {
    method: 'POST',
    body: new FormData(document.querySelector('form'))
  }
);
const { data: { file_id } } = await uploadResponse.json();

// 2. 파일 ID를 포함하여 채팅 요청
const chatResponse = await fetch('http://localhost:3000/ai/chat', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    message: 'Please analyze this image',
    model: 'gpt-5-mini',
    file_id: file_id  // ← 업로드된 파일 ID 사용
  })
});

// 3. 스트림 처리...
```

#### Python 예제

```python
import requests
import json

# 채팅 스트림 요청
response = requests.post(
    'http://localhost:3000/ai/chat',
    json={
        'message': 'Hello, how are you?',
        'model': 'gpt-5-mini'
    },
    stream=True
)

# SSE 스트림 처리
for line in response.iter_lines():
    if line:
        if line.startswith(b'data: '):
            event = json.loads(line[6:])

            if event.get('type') == 'response.output_text.delta':
                print(event.get('delta'), end='', flush=True)
            elif event.get('type') == 'response.completed':
                print(f"\n\n토큰 사용: {event['response']['usage']['total_tokens']}")
```

---

### 3️⃣ 응답 조회

이전에 생성된 응답을 ID로 조회합니다.

#### 요청

```
GET /ai/response
```

#### 쿼리 파라미터

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `id` | string | ✅ | 조회할 응답 ID (채팅 응답에서 반환) |
| `model` | string | ❌ | 조회할 제공자 결정 (기본값: AI_PROVIDER) |

#### 성공 응답 (200 OK)

```json
{
  "success": true,
  "data": {
    "id": "resp_xyz123",
    "content": "이것은 저장된 AI의 응답입니다.",
    "model": "gpt-5-mini",
    "provider": "openai",
    "usage": {
      "input_tokens": 10,
      "output_tokens": 25,
      "total_tokens": 35
    },
    "timestamp": "2024-11-16T12:34:56.789Z"
  },
  "metadata": {
    "provider": "openai",
    "timestamp": "2024-11-16T12:35:00.000Z"
  }
}
```

#### 실패 응답 (400/404)

**경우 1: 응답 ID 누락**
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "MISSING_PARAM",
    "message": "응답 ID가 제공되지 않았습니다"
  }
}
```

**경우 2: 응답을 찾을 수 없음**
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "RESPONSE_NOT_FOUND",
    "message": "응답을 찾을 수 없습니다: resp_invalid"
  }
}
```

#### cURL 예제

```bash
curl -X GET "http://localhost:3000/ai/response?id=resp_xyz123&model=gpt-5-mini"
```

#### JavaScript 예제

```javascript
const responseId = "resp_xyz123";
const model = "gpt-5-mini";

const response = await fetch(
  `http://localhost:3000/ai/response?id=${responseId}&model=${model}`
);

const result = await response.json();
console.log(result.data.content);
console.log(`토큰 사용: ${result.data.usage.total_tokens}`);
```

---

## 에러 처리

### HTTP 상태 코드

| 상태 코드 | 의미 | 예 |
|----------|------|-----|
| `200 OK` | 성공 | 채팅 스트림 시작 |
| `201 Created` | 리소스 생성 | 파일 업로드 성공 |
| `400 Bad Request` | 잘못된 요청 | 파일 미제공, 잘못된 모델 |
| `404 Not Found` | 리소스 없음 | 응답 ID 미존재 |
| `500 Internal Server Error` | 서버 에러 | API 키 오류, 네트워크 오류 |

### 일반적인 에러 코드

| 코드 | 설명 | 해결방법 |
|------|------|---------|
| `MISSING_FILE` | 파일이 제공되지 않음 | form-data로 file 필드 전송 확인 |
| `UNSUPPORTED_FORMAT` | 지원하지 않는 파일 형식 | jpg, jpeg, png, webp 형식만 지원 |
| `INVALID_MODEL` | 지원하지 않는 모델명 | 지원 모델 목록 확인 |
| `CHAT_ERROR` | 채팅 중 오류 발생 | 에러 메시지 확인, API 키 검증 |
| `RESPONSE_NOT_FOUND` | 응답 ID 미존재 | 올바른 응답 ID 확인 |
| `FILE_NOT_FOUND` | 파일을 찾을 수 없음 | 파일 ID 만료 확인 (7일) |

### 에러 응답 예제

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "INVALID_MODEL",
    "message": "지원하지 않는 모델: invalid-model. 지원 모델: gpt-5-mini, gpt-5, claude-3-5-sonnet"
  }
}
```

---

## 사용 예제

### 시나리오 1: 텍스트 기반 채팅

사용자가 간단한 질문을 하고 AI 응답을 받습니다.

```bash
# 요청
curl -X POST http://localhost:3000/ai/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "TypeScript의 제네릭에 대해 설명해주세요", "model": "gpt-5-mini"}' \
  -N

# 응답 (SSE 스트림)
data: {"type":"response.created","response":{"id":"resp_abc123"}}
data: {"type":"response.output_text.delta","delta":"TypeScript의 제네릭은..."}
data: {"type":"response.completed","response":{"id":"resp_abc123","content":"TypeScript의 제네릭은 ...","usage":{"total_tokens":150}}}
```

### 시나리오 2: 이미지 분석

사용자가 이미지를 업로드하고 분석을 요청합니다.

```bash
# 1단계: 파일 업로드
curl -X POST http://localhost:3000/ai/upload?model=gpt-4o \
  -F "file=@diagram.png"

# 응답:
# {"success": true, "data": {"file_id": "file-xyz123"}}

# 2단계: 이미지 분석 요청
curl -X POST http://localhost:3000/ai/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "이 다이어그램을 분석해주세요",
    "model": "gpt-4o",
    "file_id": "file-xyz123"
  }' \
  -N

# 응답: SSE 스트림으로 분석 내용 전달
```

### 시나리오 3: 대화 이어가기

사용자가 이전 응답을 기반으로 새로운 질문을 합니다.

```bash
# 이전 채팅 후 응답 ID: resp_abc123

# 후속 질문
curl -X POST http://localhost:3000/ai/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "그럼 좀 더 자세히 설명해주세요",
    "model": "gpt-5-mini",
    "previous_response_id": "resp_abc123"
  }' \
  -N

# 응답: 이전 대화를 참고한 후속 응답 제공
```

### 시나리오 4: 저장된 응답 조회

이전에 생성된 응답을 다시 조회합니다.

```bash
curl -X GET "http://localhost:3000/ai/response?id=resp_abc123&model=gpt-5-mini"

# 응답:
# {"success": true, "data": {"id": "resp_abc123", "content": "...", "usage": {"total_tokens": 150}}}
```



### (NEW) MSA AI 서버 최신 내용(아래)
# ErrorCode 가이드

이 문서는 AI API Server에서 반환하는 오류 응답의 `errorCode` 값과 의미를 정리합니다.

- 소스(기준): `src/common/exceptions/ai.exceptions.ts`
- 일부 `errorCode`는 호출부에서 직접 지정합니다.
- 오류 응답 형식: `docs/API_RESPONSE_FORMAT.md`

---

## ErrorCode 목록

| ErrorCode | HTTP 상태 | 의미 | `details` 예시(선택) |
|---|---:|---|---|
| `INVALID_MODEL` | 400 | 요청한 모델명이 유효하지 않음 | `requestedModel`, `supportedModels` |
| `EMPTY_MESSAGE` | 400 | 채팅 메시지 내용이 비어있음 | - |
| `INVALID_PROVIDER` | 400 | 요청한 AI 제공자가 유효하지 않음 | `provider` |
| `UNSUPPORTED_FILE_FORMAT` | 400 | 업로드 파일 확장자가 지원되지 않음 | `fileExtension`, `supportedFormats` |
| `PROVIDER_UNAVAILABLE` | 503 | AI 제공자 서비스 사용 불가(점검/장애 등) | `provider`, `reason` |
| `OPERATION_NOT_SUPPORTED` | 501 | 해당 제공자가 기능을 지원하지 않음(예: `retrieveResponse`) | `provider`, `operation` |
| `STREAM_CONNECTION_FAILED` | 502 | 스트림 연결 실패 | `provider`, `reason` |
| `STREAM_INTERRUPTED` | 500 | 스트림 중단(연결 끊김/서버 중단 등) | `provider`, `reason` |
| `STREAM_TIMEOUT` | 504 | 스트림 응답 시간 초과 | `provider`, `timeoutMs` |
| `STREAM_PARSING_FAILED` | 500 | 스트림 이벤트 파싱 실패 | `provider`, `eventType`, `reason` |
| `FILE_PROCESSING_FAILED` | 500 | 파일 업로드/처리 중 오류 | `filename`, `reason` |
| `EXTERNAL_SERVICE_FAILED` | 502 | 외부 서비스(업스트림) 호출 실패 | `serviceName`, `reason` |
| `INTERNAL_SERVER_ERROR` | 500 | 내부 서버 오류(예상치 못한 오류) | (상황별 상이) |

---

## `details` 필드 가이드

- `details`는 선택이며, 클라이언트가 분기 처리/디버깅에 활용할 수 있는 추가 컨텍스트를 제공합니다.
- `details`의 키/구조는 오류 종류에 따라 달라질 수 있습니다.


# API 응답 형식 가이드

이 문서는 AI API Server의 정상 응답과 오류 응답 형식을 설명합니다.

---

## 목차

1. [정상 응답](#정상-응답)
   - [채팅 API (SSE 스트리밍)](#1-채팅-api-sse-스트리밍)
   - [파일 업로드 API](#2-파일-업로드-api)
   - [응답 조회 API](#3-응답-조회-api)
2. [오류 응답](#오류-응답)
   - [오류 응답 구조](#오류-응답-구조)
   - [오류 코드 목록](#오류-코드-목록)
   - [HTTP 상태 코드별 오류](#http-상태-코드별-오류)
3. [SSE 스트리밍 오류 처리](#sse-스트리밍-오류-처리)

---

## 정상 응답

### 1. 채팅 API (SSE 스트리밍)

**엔드포인트**: `POST /ai/chat`

**Content-Type**: `text/event-stream`

채팅 API는 Server-Sent Events(SSE)를 통해 실시간 스트리밍 응답을 제공합니다.

#### 스트림 이벤트 형식

각 이벤트는 `data: {JSON}\n\n` 형식으로 전송됩니다.

##### 응답 이벤트 (텍스트 청크)

```json
data: {"type":"response","data":"안녕"}

data: {"type":"response","data":"하세요"}

data: {"type":"response","data":"! 무엇을 도와드릴까요?"}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `type` | `"response"` | 이벤트 타입 (텍스트 응답) |
| `data` | `string` | AI가 생성한 텍스트 청크 |

##### 사용량 이벤트 (스트림 종료 시)

```json
data: {"type":"usage","data":{"input_tokens":25,"output_tokens":42,"total_tokens":67,"response_id":"resp_abc123xyz"}}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `type` | `"usage"` | 이벤트 타입 (사용량 정보) |
| `data.input_tokens` | `number` | 입력(프롬프트) 토큰 수 |
| `data.output_tokens` | `number` | 출력(응답) 토큰 수 |
| `data.total_tokens` | `number` | 총 토큰 수 |
| `data.response_id` | `string` | 응답 고유 ID (OpenAI만 지원) |

#### 전체 스트림 예시

```
data: {"type":"response","data":"안녕"}

data: {"type":"response","data":"하세요! "}

data: {"type":"response","data":"오늘 "}

data: {"type":"response","data":"날씨가 좋네요."}

data: {"type":"usage","data":{"input_tokens":10,"output_tokens":15,"total_tokens":25,"response_id":"resp_abc123"}}
```

---

### 2. 파일 업로드 API

**엔드포인트**: `POST /ai/upload?model={모델명}`

**Content-Type**: `application/json`

#### 성공 응답

```json
{
  "success": true,
  "data": {
    "file_id": "file-abc123xyz"
  },
  "metadata": {
    "provider": "openai",
    "timestamp": "2025-12-16T10:30:00.000Z"
  }
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `success` | `boolean` | 요청 성공 여부 |
| `data.file_id` | `string` | 업로드된 파일 ID (채팅 시 사용) |
| `metadata.provider` | `string` | AI 제공자 (`openai`, `claude`, `gemini`) |
| `metadata.timestamp` | `string` | 업로드 시각 (ISO 8601) |

> **참고**: Gemini의 경우 `file_id` 대신 `uri` 형식이 반환됩니다.

---

### 3. 응답 조회 API

**엔드포인트**: `GET /ai/response?id={응답ID}&model={모델명}`

**Content-Type**: `application/json`

> **참고**: 현재 OpenAI만 응답 조회를 지원합니다.

#### 성공 응답

```json
{
  "success": true,
  "data": {
    "id": "resp_abc123xyz",
    "content": "AI가 생성한 전체 응답 텍스트입니다.",
    "model": "gpt-5-mini",
    "provider": "openai",
    "usage": {
      "input_tokens": 25,
      "output_tokens": 42,
      "total_tokens": 67,
      "response_id": "resp_abc123xyz"
    },
    "timestamp": "2025-12-16T10:30:00.000Z"
  },
  "metadata": {
    "provider": "openai",
    "timestamp": "2025-12-16T10:35:00.000Z"
  }
}
```

---

## 오류 응답

### 오류 응답 구조

모든 오류는 일관된 JSON 구조로 반환됩니다.

```json
{
  "errorCode": "ERROR_CODE",
  "message": "사람이 읽을 수 있는 오류 메시지",
  "details": {
    "추가": "정보"
  },
  "timestamp": "2025-12-16T10:30:00.000Z"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `errorCode` | `string` | ✅ | 프로그래밍 방식으로 처리할 수 있는 오류 코드 |
| `message` | `string` | ✅ | 사용자에게 표시할 오류 메시지 (한글) |
| `details` | `object` | ❌ | 오류에 대한 추가 컨텍스트 정보 |
| `timestamp` | `string` | ✅ | 오류 발생 시각 (ISO 8601) |

---

### 오류 코드 목록

> 전체 ErrorCode 목록과 의미는 `docs/ERROR_CODES.md`를 참고하세요.

#### 유효성 검증 오류 (400 Bad Request)

| 오류 코드 | 메시지 | 설명 |
|-----------|--------|------|
| `INVALID_MODEL` | 지원하지 않는 모델입니다: {model} | 요청한 모델명이 유효하지 않음 |
| `EMPTY_MESSAGE` | 메시지 내용이 비어있습니다. | 채팅 메시지가 비어있음 |
| `INVALID_PROVIDER` | Invalid AI provider: {provider} | 요청한 제공자 값이 유효하지 않음 |
| `UNSUPPORTED_FILE_FORMAT` | 지원하지 않는 형식: {ext} | 업로드 파일 확장자가 지원되지 않음 |

**예시: 잘못된 모델 요청**

```json
{
  "errorCode": "INVALID_MODEL",
  "message": "지원하지 않는 모델입니다: gpt-unknown",
  "details": {
    "requestedModel": "gpt-unknown",
    "supportedModels": ["gpt-4o", "gpt-4o-mini", "gpt-5", "gpt-5-mini", "gpt-5-nano", "gpt-5.1", "claude-haiku-4-5", "gemini-2.5-flash", "gemini-2.5-flash-lite", "gemini-3-pro-preview"]
  },
  "timestamp": "2025-12-16T10:30:00.000Z"
}
```

---

#### 기능 미지원 (501 Not Implemented)

| 오류 코드 | 메시지 | 설명 |
|-----------|--------|------|
| `OPERATION_NOT_SUPPORTED` | {provider} 제공자는 해당 기능을 지원하지 않습니다. | 제공자에서 해당 기능을 구현하지 않음 |

**예시: 응답 조회 미지원**

```json
{
  "errorCode": "OPERATION_NOT_SUPPORTED",
  "message": "Claude 제공자는 응답 검색을 지원하지 않습니다. 이 기능을 위해 OpenAI 제공자를 사용하세요.",
  "details": {
    "provider": "claude",
    "operation": "retrieveResponse"
  },
  "timestamp": "2025-12-16T10:30:00.000Z"
}
```

---

#### 제공자 오류 (503 Service Unavailable)

| 오류 코드 | 메시지 | 설명 |
|-----------|--------|------|
| `PROVIDER_UNAVAILABLE` | AI 제공자 서비스를 사용할 수 없습니다: {provider} | AI 제공자 API가 다운되었거나 점검 중 |

**예시: 제공자 서비스 불가**

```json
{
  "errorCode": "PROVIDER_UNAVAILABLE",
  "message": "AI 제공자 서비스를 사용할 수 없습니다: openai",
  "details": {
    "provider": "openai",
    "reason": "API 서버 점검 중"
  },
  "timestamp": "2025-12-16T10:30:00.000Z"
}
```

---

#### 스트리밍 오류 (500/502/504)

| 오류 코드 | HTTP 상태 | 메시지 |
|-----------|-----------|--------|
| `STREAM_CONNECTION_FAILED` | 502 | 스트림 연결에 실패했습니다: {provider} |
| `STREAM_INTERRUPTED` | 500 | 스트림이 중단되었습니다: {provider} |
| `STREAM_TIMEOUT` | 504 | 스트림 응답 시간이 초과되었습니다: {provider} |
| `STREAM_PARSING_FAILED` | 500 | 스트림 이벤트 파싱에 실패했습니다: {provider} |

**예시: 스트림 타임아웃**

```json
{
  "errorCode": "STREAM_TIMEOUT",
  "message": "스트림 응답 시간이 초과되었습니다: openai",
  "details": {
    "provider": "openai",
    "timeoutMs": 30000
  },
  "timestamp": "2025-12-16T10:30:00.000Z"
}
```

---

#### 리소스 오류 (500)

| 오류 코드 | 메시지 | 설명 |
|-----------|--------|------|
| `FILE_PROCESSING_FAILED` | 파일 처리 중 오류가 발생했습니다: {filename} | 파일 업로드/처리 실패 |

**예시: 파일 처리 실패**

```json
{
  "errorCode": "FILE_PROCESSING_FAILED",
  "message": "파일 처리 중 오류가 발생했습니다: document.pdf",
  "details": {
    "filename": "document.pdf",
    "reason": "OpenAI 파일 업로드 실패: 네트워크 연결 시간 초과"
  },
  "timestamp": "2025-12-16T10:30:00.000Z"
}
```

---

#### 내부 오류 (500/502)

| 오류 코드 | HTTP 상태 | 메시지 |
|-----------|-----------|--------|
| `EXTERNAL_SERVICE_FAILED` | 502 | 외부 서비스 연결에 실패했습니다: {serviceName} |
| `INTERNAL_SERVER_ERROR` | 500 | 내부 서버 오류가 발생했습니다: {message} |

**예시: 외부 서비스 연결 실패**

```json
{
  "errorCode": "EXTERNAL_SERVICE_FAILED",
  "message": "외부 서비스 연결에 실패했습니다: OpenAI API",
  "details": {
    "serviceName": "OpenAI API",
    "reason": "네트워크 연결 시간 초과"
  },
  "timestamp": "2025-12-16T10:30:00.000Z"
}
```

---

### HTTP 상태 코드별 오류

| 상태 코드 | 의미 | 관련 오류 |
|-----------|------|-----------|
| `400` | Bad Request | `INVALID_MODEL`, `EMPTY_MESSAGE`, `INVALID_PROVIDER`, `UNSUPPORTED_FILE_FORMAT` |
| `501` | Not Implemented | `OPERATION_NOT_SUPPORTED` |
| `500` | Internal Server Error | `STREAM_INTERRUPTED`, `STREAM_PARSING_FAILED`, `FILE_PROCESSING_FAILED`, `INTERNAL_SERVER_ERROR` |
| `502` | Bad Gateway | `STREAM_CONNECTION_FAILED`, `EXTERNAL_SERVICE_FAILED` |
| `503` | Service Unavailable | `PROVIDER_UNAVAILABLE` |
| `504` | Gateway Timeout | `STREAM_TIMEOUT` |

---

## SSE 스트리밍 오류 처리

채팅 API에서 스트리밍 중 오류가 발생하면, 특별한 오류 이벤트가 전송됩니다.

### 스트리밍 오류 이벤트

```json
data: {"type":"error","error":{"code":"CHAT_ERROR","message":"스트림 채팅 실패: 연결이 끊어졌습니다"}}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `type` | `"error"` | 이벤트 타입 (오류) |
| `error.code` | `string` | 오류 코드 |
| `error.message` | `string` | 오류 메시지 |

### 클라이언트 처리 예시 (JavaScript)

```javascript
const eventSource = new EventSource('/ai/chat');

eventSource.onmessage = (event) => {
  const data = JSON.parse(event.data);

  switch (data.type) {
    case 'response':
      // 텍스트 청크 처리
      console.log('텍스트:', data.data);
      break;

    case 'usage':
      // 사용량 정보 (스트림 종료)
      console.log('토큰 사용량:', data.data);
      eventSource.close();
      break;

    case 'error':
      // 오류 처리
      console.error('오류 발생:', data.error.message);
      eventSource.close();
      break;
  }
};

eventSource.onerror = (error) => {
  console.error('SSE 연결 오류:', error);
  eventSource.close();
};
```

---

## 오류 계층 구조

```
BaseAiException (기본 예외)
│
├── AiValidationException (400)
│   ├── InvalidModelException
│   └── EmptyMessageException
│
├── AiProviderException (501/502/503)
│   └── ProviderUnavailableException
│
├── AiStreamException (500/502/504)
│   ├── StreamConnectionException
│   ├── StreamInterruptedException
│   ├── StreamTimeoutException
│   └── StreamParsingException
│
├── AiResourceException
│   └── FileProcessingException
│
└── AiInternalException (500/502)
    ├── ExternalServiceException
    └── InternalServerException
```

---

## 버전 정보

- **문서 버전**: 1.0.0
- **최종 업데이트**: 2025-12-16
- **작성자**: AI API Server 팀
