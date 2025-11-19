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
