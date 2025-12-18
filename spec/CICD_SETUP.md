# CI/CD 파이프라인 설정 가이드

이 문서는 GitHub Actions를 통한 CI/CD 파이프라인 설정 방법을 안내합니다.

---

## 📋 파이프라인 개요

- PR: 테스트(CI)만 실행 (`main`, `dev`, `release/**` 대상으로)
- main 브랜치 push: 테스트 → 이미지 빌드/푸시 → 매니페스트(values.yaml) 이미지 태그 업데이트까지 실행(CICD)

### 워크플로우 구조
```
┌─────────────┐
│ Push to PR  │ → ┌─────────────┐
│   or main   │   │  Run Tests  │
└──────┬──────┘   └──────┬──────┘
       │                │
       ├── main push ───┘
       ▼
┌──────────────────┐
│  Build & Push    │  ← ghcr.io/<repo_owner>/main-server:main, main-<sha>, latest
└──────┬───────────┘
       ▼
┌──────────────────┐
│ Update Manifest  │  ← k8s-manifests/values.yaml의 springApp.image 태그를 <sha>로 교체/커밋/푸시
└──────────────────┘
```

### 컨테이너 런타임 호환성

빌드된 이미지는 **OCI (Open Container Initiative) 표준**을 따르며, 다음 런타임에서 모두 실행 가능합니다:

- ✅ **containerd** (Kubernetes 기본 런타임)
- ✅ **CRI-O** (Red Hat OpenShift 기본 런타임)
- ✅ **Docker Engine** (로컬 개발 환경)
- ✅ 기타 모든 OCI 호환 컨테이너 런타임

### 트리거 조건
- **Push**: `main`, `dev`, `release/**` 브랜치에 푸시될 때
- **Pull Request**: `main`, `dev`, `release/**` 브랜치로의 PR 생성 시

---

## 🔐 GitHub Secrets / Variables

필수(매니페스트 업데이트용 PAT 필요):

| 이름 | 용도 | 예시 |
|------|------|------|
| `MANIFEST_REPO` | 매니페스트 리포지토리 (`owner/repo`) | `AI-HUB-KR/k8s-manifests` |
| `MANIFEST_REPO_TOKEN` | 매니페스트 리포지토리 쓰기용 PAT | `ghp_xxx` |

선택(GHCR 퍼블리시가 org 정책으로 GITHUB_TOKEN에 막혀 있을 때만):

| 이름 | 용도 | 예시 |
|------|------|------|
| `GHCR_PAT` | GHCR 로그인용 PAT (`write:packages`) | `ghp_xxx` |

> GHCR 푸시는 기본적으로 `secrets.GITHUB_TOKEN` + `permissions: packages: write`로 동작하며, 리포 소유자 네임스페이스(`github.repository_owner`)로 푸시합니다. Org 정책이 막으면 `GHCR_PAT`을 추가해 `docker/login-action`에 공급하세요.

---

## 🔑 GitHub Personal Access Token (PAT) 생성

매니페스트 리포지토리를 업데이트하려면 PAT가 필요합니다.

### PAT 생성 단계

1. **GitHub 프로필** → Settings → Developer settings → Personal access tokens → Tokens (classic)
2. **Generate new token (classic)** 클릭
3. **설정**:
   - **Note**: `AI-HUB-BE CI/CD Token`
   - **Expiration**: `No expiration` (또는 적절한 기간)
   - **Scopes** (필수 권한):
     - ✅ `repo` (Full control of private repositories)
     - ✅ `workflow` (Update GitHub Action workflows)
4. **Generate token** 클릭
5. 생성된 토큰을 **즉시 복사** (다시 볼 수 없음)
6. 복사한 토큰을 `MANIFEST_REPO_TOKEN` Secret으로 등록

---

## 📦 GHCR (GitHub Container Registry) 설정

### 자동 인증
- `GITHUB_TOKEN`이 자동으로 제공되므로 별도 설정 불필요
- 워크플로우에서 자동으로 GHCR에 로그인

### 이미지 접근 권한 설정 (선택)

1. **GitHub 프로필** → Packages → 해당 이미지 선택
2. **Package settings** → **Danger Zone** → Change visibility
3. **Public** 또는 **Private** 선택

---

## 📦 OCI 컨테이너 이미지 전략

- 태그 전략(`docker/metadata-action`):  
  - `main` (브랜치 태그) → `ghcr.io/<repo_owner>/main-server:main`  
  - `main-<sha>` (브랜치-SHA) → `ghcr.io/<repo_owner>/main-server:main-a1b2c3d`  
  - `latest` (기본 브랜치만)
- 값은 `github.repository_owner` 기준으로 자동 네임스페이스 결정. `IMAGE_NAME` 기본값: `<repo_owner>/main-server`.

---

## 📁 매니페스트 리포지토리 구조

Helm 차트로 관리됩니다.

```
k8s-manifests/
├── Chart.yaml       # 차트 메타데이터
├── values.yaml      # 네임스페이스/DB/애플리케이션 기본값 (springApp.image 포함)
└── templates/       # Kubernetes 리소스 템플릿
```

`values.yaml` 내 `springApp.image`가 GitHub Actions에서 자동으로 커밋/푸시되며, Helm 템플릿은 이 값을 참조해 배포 시점 이미지 태그를 가져갑니다. 예시:
```yaml
springApp:
  image: ghcr.io/username/main-server:latest  # ← CI에서 SHA로 자동 교체

namespace: ai-hub
database:
  host: postgres
```

---

## 🔧 워크플로우 커스터마이징

### 매니페스트 업데이트 경로 수정

`.github/workflows/cicd.yaml` 파일에서 `update-manifest` Job의 경로를 수정하세요:


### 브랜치 전략 변경

다른 브랜치를 트리거로 사용하려면:

```yaml
on:
  push:
    branches:
      - main
      - staging  # ← 추가
```

---

## ☸️ Kubernetes Secrets 설정

애플리케이션 실행에 필요한 환경 변수를 K8s Secret으로 관리합니다.

### Secret 생성 예시
`application.yaml` / `application-prod.yaml` 기준으로 운영에 필요한 환경 변수들을 Secret에 등록하세요.

필수(경로/자격):
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- `DEPLOYMENT_ADDRESS`, `FRONTEND_ADDRESS`
- `JWT_SECRET`
- `CORS_ALLOWED_ORIGINS`
- `AI_SERVER_URL`
- `KAKAO_CLIENT_ID`, `KAKAO_CLIENT_SECRET`

선택/기본값 있음:
- `JWT_EXPIRATION_SECOND`(기본 3600), `JWT_REFRESH_EXPIRATION_SECOND`(기본 2592000)
- `SWAGGER_ENABLED`(기본 false)

예시(`production` 네임스페이스):

> DB_URL, DB_USERNAME, DB_PASSWORD는 CloudNativePG 등에서 관리

```bash
kubectl create secret generic spring-app-secret \
  --from-literal=DEPLOYMENT_ADDRESS=https://api.aihub.com \
  --from-literal=FRONTEND_ADDRESS=https://aihub.com \
  --from-literal=JWT_SECRET=your-super-strong-secret-key \
  --from-literal=JWT_EXPIRATION_SECOND=3600 \
  --from-literal=JWT_REFRESH_EXPIRATION_SECOND=2592000 \
  --from-literal=CORS_ALLOWED_ORIGINS=https://aihub.com \
  --from-literal=AI_SERVER_URL=https://ai.aihub.com \
  --from-literal=KAKAO_CLIENT_ID=your-kakao-client-id \
  --from-literal=KAKAO_CLIENT_SECRET=your-kakao-client-secret \
  --from-literal=SWAGGER_ENABLED=false \
  --namespace=production
```


## 🚀 파이프라인 실행

### 자동 실행
```bash
git add .
git commit -m "feat: add new feature"
git push origin main
```

### 수동 실행
GitHub 리포지토리 → Actions → CI/CD Pipeline → Run workflow


---

## 📊 파이프라인 모니터링

### GitHub Actions 대시보드

- **성공률**: Actions 탭에서 워크플로우 실행 기록 확인
- **빌드 시간**: 각 Job별 실행 시간 분석
- **실패 원인**: 로그에서 에러 메시지 확인



---

## 📚 참고 자료

- [GitHub Actions 문서](https://docs.github.com/en/actions)
- [GHCR 문서](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry)
- [Kustomize 문서](https://kustomize.io/)
- [Sealed Secrets](https://github.com/bitnami-labs/sealed-secrets)
- [Docker 멀티스테이지 빌드](https://docs.docker.com/build/building/multi-stage/)

---

## ✅ 체크리스트

배포 전 확인사항:

- [ ] GitHub Secrets 설정 완료 (`MANIFEST_REPO`, `MANIFEST_REPO_TOKEN`)
- [ ] PAT 생성 및 권한 확인 (`repo`, `workflow`)
- [ ] 매니페스트 리포지토리 경로 확인
- [ ] K8s Secrets 생성 완료 (환경 변수)
- [ ] Dockerfile 테스트 완료
- [ ] 워크플로우 경로 수정 (매니페스트 리포지토리 구조에 맞게)
- [ ] 테스트 실행 확인 (`./gradlew test`)
- [ ] Docker 이미지 태그 전략 검토

---

**문의사항**이 있으면 팀 리드에게 연락하세요.
