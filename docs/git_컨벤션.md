# Git 컨벤션

Compass 프로젝트에서 커밋, 브랜치, PR, 배포 반영을 할 때 지키는 기준입니다.

## 기본 원칙

- 한 커밋에는 하나의 목적만 담습니다.
- 기능 변경, 버그 수정, 문서 수정, 리팩토링을 가능하면 섞지 않습니다.
- `.env`, API 키, 서비스 계정 JSON, DB 비밀번호는 절대 커밋하지 않습니다.
- `main` 브랜치는 항상 실행 가능한 상태를 유지합니다.
- 배포 전에 로컬 또는 CI에서 최소 검증을 통과시킵니다.

## 브랜치 이름

개인 작업 브랜치는 아래 형식을 권장합니다.

```text
type/short-description
```

예시:

```text
feat/plan-delete
fix/oauth-redirect
docs/readme-update
refactor/plan-detail-page
test/plan-permission
```

자주 쓰는 타입:

| 타입 | 의미 |
| --- | --- |
| `feat` | 새 기능 추가 |
| `fix` | 버그 수정 |
| `docs` | 문서 수정 |
| `style` | 코드 포맷, UI 스타일 수정 |
| `refactor` | 기능 변화 없는 구조 개선 |
| `test` | 테스트 추가/수정 |
| `chore` | 설정, 빌드, 의존성, 기타 작업 |
| `deploy` | 배포 설정 수정 |

## 커밋 메시지

커밋 메시지는 아래 형식을 사용합니다.

```text
type: 작업 내용 요약
```

예시:

```text
docs: README와 운영 문서 최신화
feat: 일정 삭제 기능 추가
fix: OAuth 콜백 인증 오류 수정
refactor: 일정 상세 페이지 컴포넌트 분리
test: 타인 일정 접근 차단 테스트 추가
chore: GitHub Actions CI 추가
```

## 커밋 타입 기준

| 타입 | 사용할 때 |
| --- | --- |
| `feat` | 사용자가 볼 수 있는 기능이 새로 생겼을 때 |
| `fix` | 오류, 예외, 잘못된 동작을 고쳤을 때 |
| `docs` | README, docs, 주석 등 문서만 바뀌었을 때 |
| `style` | CSS, 레이아웃, 포맷처럼 동작 변화가 거의 없을 때 |
| `refactor` | 코드 구조를 바꾸지만 기능은 그대로일 때 |
| `test` | 테스트 코드를 추가하거나 수정했을 때 |
| `chore` | 설정 파일, CI, 패키지, 빌드 스크립트 작업 |
| `deploy` | Docker, Caddy, Vercel, EC2 배포 설정 작업 |

## 좋은 커밋 예시

```text
feat: 관리자 쿼터 수정 기능 추가
fix: 일정 삭제 시 트랜잭션 누락 오류 수정
docs: Vertex AI 환경변수 설명 추가
refactor: AI 일정 생성 프롬프트 상수 분리
test: planId 소유자 검증 테스트 추가
```

## 나쁜 커밋 예시

```text
수정
업데이트
작업함
오류 고침
최종
진짜최종
```

왜 나쁜가:

- 무엇을 바꿨는지 알 수 없습니다.
- 포트폴리오 검토자가 작업 흐름을 파악하기 어렵습니다.
- 나중에 문제가 생겼을 때 되돌릴 커밋을 찾기 어렵습니다.

## Push 전 체크리스트

커밋하기 전에 확인합니다.

```bash
git status
```

비밀 파일이 포함되지 않았는지 확인합니다.

```bash
git diff --name-only --cached
```

가능하면 아래 검증을 실행합니다.

```bash
cd backend
./gradlew test
```

```bash
cd compass-frontend
npm run lint
npm run build
```

문서만 수정한 경우에는 최소한 아래는 확인합니다.

```bash
git diff --check
```

## 작업 흐름

일반적인 작업 순서입니다.

```bash
git status
git add <파일>
git commit -m "type: 작업 내용 요약"
git pull origin main
git push origin main
```

혼자 작업하는 포트폴리오 프로젝트라면 `main`에 직접 push해도 됩니다.  
다만 실무 스타일을 보여주고 싶다면 기능별 브랜치를 만들고 Pull Request로 합치는 방식이 더 좋습니다.

```bash
git checkout -b feat/example
git add <파일>
git commit -m "feat: 예시 기능 추가"
git push origin feat/example
```

그 다음 GitHub에서 Pull Request를 생성합니다.

## 배포 반영 흐름

프론트엔드는 GitHub main에 push되면 Vercel이 자동 배포합니다.

백엔드는 EC2에서 직접 최신 코드를 당겨와야 합니다.

```bash
ssh -i "compass-key.pem" ubuntu@<EC2_PUBLIC_IP>
cd ~/KHSBE-AIT
git pull origin main
docker compose -f docker-compose.prod.yml up -d --build --force-recreate backend caddy
docker compose -f docker-compose.prod.yml ps
```

## 커밋하지 말아야 할 파일

아래 파일은 절대 GitHub에 올리지 않습니다.

```text
.env
backend/.env
compass-frontend/.env
secrets/gcp-sa.json
*.pem
*.key
```

이미 실수로 올렸다면 파일을 삭제하는 것만으로는 부족합니다.  
노출된 API 키, OAuth Secret, 서비스 계정 키는 즉시 폐기하고 새로 발급해야 합니다.

## 포트폴리오 관점에서 좋은 Git 기록

면접관이 GitHub를 볼 때 좋은 인상을 주는 기록입니다.

- 기능 단위로 커밋이 나뉘어 있음
- 커밋 메시지만 봐도 작업 의도가 보임
- README와 docs가 실제 코드와 맞음
- CI가 통과한 이력이 있음
- 이슈 또는 PR에 “왜 이 기능을 만들었는지”가 남아 있음

## 추천 규칙

Compass에서는 아래 정도만 지켜도 충분합니다.

```text
feat: 기능 추가
fix: 버그 수정
docs: 문서 수정
refactor: 구조 개선
test: 테스트 추가
chore: 설정/기타
deploy: 배포 설정
```
