# 배포 가이드: Render + Neon + Vercel

기준일: 2026-11-05. 현재 코드와 이번 배포에서 확인한 절차를 기록합니다.
Render Live, Vercel Production Ready, 공개 프론트의 Render API 주소 반영을 확인했습니다.
Google 로그인 완료, AI 생성, DB 저장/편집/삭제의 전체 기능 검증은 별도입니다.

## 현재 구성

| 역할 | 서비스 / 주소 |
| --- | --- |
| 프론트 | Vercel: https://www.compass-travel.site |
| 프론트 기본 주소 | https://khsbe-ait.vercel.app |
| 백엔드 | Render: https://khsbe-ait.onrender.com |
| DB | Neon PostgreSQL, Singapore |
| AI | Google Cloud Vertex 기반 Google GenAI |

EC2, Caddy, 로컬 Docker 설치는 이 배포에 필요하지 않습니다.
기존 EC2 파일은 복구 참고용으로 유지하며 현재 절차와 혼용하지 않습니다.
Render 프로필에서 Redis 자동 설정은 제외됩니다. 현재 Places 캐시는 서버 메모리에 있습니다.

## 1. 코드 준비

다음 파일을 배포 브랜치에 커밋하고 push합니다. 로컬 파일만 수정하면 Render에 반영되지 않습니다.

- `backend/Dockerfile.render`
- `backend/.dockerignore`
- `backend/src/main/resources/application-render.yml`
- 변경한 애플리케이션 코드 및 필요한 설정

`.env`, 서비스 계정 JSON, SSH 키, 로컬 프로젝트 압축파일은 커밋하지 않습니다.
CI 통과와 자동 배포 차단은 별개입니다. 배포 게이트를 별도로 설정하지 않았다면 CI 결과를 확인하고 수동 배포합니다.

## 2. Neon DB 생성

Free 프로젝트를 생성하고 Render와 가까운 Singapore를 선택합니다.
Postgres만 사용하며 Auth, Functions, AI Gateway 등은 필요하지 않습니다.
Connect에서 Database와 Role을 확인하고 Connection pooling을 끈 직접 연결 주소를 사용합니다.
이 토글은 표시할 연결 주소를 선택하는 옵션이지 DB 전체 설정 변경이 아닙니다.

```text
DB_JDBC_URL=jdbc:postgresql://YOUR_NEON_HOST:5432/neondb?sslmode=require
DB_USERNAME=neondb_owner
DB_PASSWORD=<Neon 비밀번호>
```

실제 DB/Role 이름이 다르면 해당 값으로 바꿉니다. JDBC URL에 비밀번호, `psql`, 따옴표를 넣지 않습니다.
새 테스트 DB는 현재 `ddl-auto=update`로 초기화됩니다. 중요한 데이터에는 백업과 버전별 마이그레이션이 필요합니다.

## 3. Render Web Service

GitHub 저장소 연결 후 다음을 지정합니다.

| 항목 | 값 |
| --- | --- |
| Language | Docker |
| Branch | 배포 코드가 있는 main |
| Region | Singapore |
| Root Directory | backend |
| Dockerfile Path | ./Dockerfile.render |
| Docker Build Context Directory | . |
| Compute | Free / $0 |
| Health Check Path | 빈칸: 기본 TCP 검사 |
| Docker Command / Pre-Deploy Command | 빈칸 |

Docker 관련 입력란은 Advanced에 있을 수 있습니다. UI가 Root Directory 접두사 `backend/`를 이미 표시하면 이를 중복 입력하지 않습니다.
최종 해석은 Dockerfile=`backend/Dockerfile.render`, 빌드 컨텍스트=`backend`여야 합니다.
`/healthz` API는 구현되어 있지 않으므로 입력하지 않습니다. TCP 검사 성공은 전체 기능 정상의 증거가 아닙니다.

## 4. Render 환경변수와 Secret File

Environment에서 등록합니다. 기존 EC2 `.env`를 통째로 가져오지 않습니다.

| Key | 설정 / 의미 |
| --- | --- |
| SPRING_PROFILES_ACTIVE | render (Dockerfile에도 지정됨) |
| DB_JDBC_URL | 위 Neon JDBC URL |
| DB_USERNAME | Neon Role |
| DB_PASSWORD | Neon 비밀번호 |
| GOOGLE_CLIENT_ID | Google OAuth 클라이언트 ID |
| GOOGLE_CLIENT_SECRET | 같은 OAuth 클라이언트 Secret |
| JWT_SECRET | 충분히 긴 무작위 비밀 값. 예시 문자열 사용 금지 |
| GOOGLE_PROJECT_ID | Google Cloud 프로젝트 ID |
| GOOGLE_GENAI_VERTEX_AI | true (기본값) |
| GOOGLE_CLOUD_LOCATION | global (기본값) |
| GOOGLE_CREDENTIALS_URI | file:/etc/secrets/gcp-sa.json |
| GOOGLE_APPLICATION_CREDENTIALS | /etc/secrets/gcp-sa.json |
| GOOGLE_PLACES_API_KEY | 서버 Places API 키. 누락 시 장소 검증이 건너뛰어짐 |
| CORS_ALLOWED_ORIGINS | 허용할 프론트 origin, 쉼표 구분, 끝 슬래시 없음 |
| OAUTH2_REDIRECT_URI | 로그인 완료 후 프론트 주소 + /oauth2/redirect |
| WEATHER_API_KEY | 날씨 기능 사용 시 필요 |
| ADMIN_EMAILS | 관리자 지정 시 이메일, 쉼표 구분 |

현재 도메인 사용 예시:

```text
CORS_ALLOWED_ORIGINS=https://www.compass-travel.site,https://compass-travel.site,https://khsbe-ait.vercel.app
OAUTH2_REDIRECT_URI=https://www.compass-travel.site/oauth2/redirect
```

Secret Files에 이름 `gcp-sa.json`, 내용은 서비스 계정 JSON 전체를 등록합니다.
`GOOGLE_APPLICATION_CREDENTIALS`에는 `file:`을 붙이지 않습니다. 이번 배포에서 이 변수가 빠져 ADC 초기화가 실패했습니다.
비밀 파일은 이미지에 넣지 않고 `/etc/secrets/gcp-sa.json`에서 읽습니다.
키의 API/IP 제한이 이전 EC2에만 맞춰져 있지 않은지 확인하되 제한을 무작정 해제하지 않습니다.

### 기본값을 바꿀 때만 추가

| 변수 | 코드 기본값 |
| --- | --- |
| AI_MODEL_GENERAL | gemini-3.8-flash |
| AI_MODEL_PLAN | gemini-3.1-pro-preview |
| AI_CHAT_HISTORY_WINDOW_MESSAGES | 12 |
| PLAN_QUOTA_PER_DAY | 3 |
| CHAT_QUOTA_PER_DAY | 10 |
| GOOGLE_PLACES_LANGUAGE | ko |
| PLACES_VERIFICATION_ENABLED | true |
| PLACES_VERIFICATION_MAX_PLACES | 20 |
| PLACES_VERIFICATION_MIN_SCORE | 45 (정수 점수, 0.45가 아님) |
| PLACES_VERIFICATION_CANDIDATES_PER_QUERY | 5 |
| PLACES_VERIFICATION_CACHE_TTL_HOURS | 24 |
| DB_DDL_AUTO | update |

모델 이름은 저장소 기본값이지 공급자의 현재 가용성 보장이 아닙니다. 실제 프로젝트 접근 권한과 모델 수명주기를 확인합니다.
`PORT`는 Render 제공값을 사용하고 없으면 10000입니다. 기존 `DB_HOST`, `REDIS_HOST`는 Render 배포에 불필요합니다.
JVM 초기 설정은 힙 256MB, DB 풀 최대 3개입니다. 전체 프로세스가 512MB에 들어가는지는 부하 테스트가 필요합니다.

## 5. Google 로그인과 Vercel 연결

Google Cloud의 사용 중인 OAuth 클라이언트에서 **승인된 리디렉션 URI**에 추가합니다.

```text
https://khsbe-ait.onrender.com/login/oauth2/code/google
```

JavaScript 원본 항목이나 프론트 완료 주소와 혼동하지 않습니다.

Vercel 프로젝트 Environment Variables:

```text
VITE_API_BASE_URL=https://khsbe-ait.onrender.com
VITE_GOOGLE_MAPS_API_KEY=<브라우저 지도 API 키>
```

Production에 적용해 저장한 뒤 Deployments에서 Production 배포를 Redeploy합니다.
Vite 변수는 빌드 시 포함되므로 저장만으로 기존 JS가 바뀌지 않습니다.
캐시 해제는 매번 필수가 아닙니다. 이전 산출물 문제를 분리할 때 사용합니다.
Ready와 Production Current를 확인하고 브라우저를 강력 새로고침합니다.
VITE_ 변수는 사용자에게 노출됩니다. 서비스 계정 키나 OAuth Secret을 넣지 않습니다.
브라우저 지도 키에는 실제 프론트 도메인과 사용하는 API에 맞는 제한을 둡니다.

## 6. 변경 종류별 반영

| 변경 | 필요한 작업 |
| --- | --- |
| 백엔드 코드 | push 후 Render 자동 배포 설정 확인 또는 수동 배포 |
| Render 환경변수 / Secret File | 저장 후 재시작 또는 재배포 |
| 프론트 코드 | push 후 Vercel Production 배포 확인 |
| VITE_ 환경변수 | Vercel 저장 후 반드시 새 빌드/재배포 |
| Google OAuth 콜백 | Google Cloud에서 저장 후 로그인 재시도 |

EC2 SSH, git pull, docker compose는 현재 배포 절차가 아닙니다.

## 7. 완료 체크리스트

- [ ] Render Live, 최신 커밋 확인.
- [ ] Vercel Ready / Production Current, 새 API 주소 반영.
- [ ] Google 로그인 완료 후 올바른 프론트로 복귀.
- [ ] 일반 채팅과 짧은 일정 생성 성공.
- [ ] Places 검증이 실제 실행되고 미확인 결과를 확인.
- [ ] 저장, 새로고침, 편집, 삭제 동작 확인.
- [ ] 재배포 후에도 Neon의 저장 데이터 유지 확인.
- [ ] 관리자 권한, 다른 사용자 데이터 접근 차단 확인.
- [ ] 휴면 후 첫 접속 및 일정 생성 중 메모리 확인.

현재 로그인/쿼터/검증 관련 알려진 결함은 배포만으로 해결되지 않습니다.
통제된 테스트를 먼저 진행합니다. URL을 숨기는 것은 접근 제한이 아닙니다.

## 비용과 보관

무료 플랜은 사용량 제한·휴면·정책 변경 가능성이 있으며 영구 무중단을 보장하지 않습니다.
Vertex, Maps/Places, 사용자 도메인 갱신 비용은 별개입니다. 크레딧 만료와 API 쿼터를 확인합니다.
중요 데이터는 Neon 외부에도 백업하고 복원 테스트를 합니다. 서버 롤백은 DB를 되돌리지 않습니다.

## 관련 문서

- [장애 대응과 운영](서버_운영_런북.md)
- [로컬 실행](개발자_실행가이드.md)
- [Render Docker 문서](https://render.com/docs/docker)
- [Render 비밀 파일](https://render.com/docs/docker-secrets)
- [Render 무료 제한](https://render.com/docs/free)
- [Neon 요금 정책](https://neon.com/pricing)
