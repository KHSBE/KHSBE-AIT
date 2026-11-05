# 배포: Vercel + Render + Neon

기준: 2026-11-05 선택형 전환. 새 코드 배포는 아직 하지 않았습니다. 로컬 Docker는 필수가 아닙니다.

## 먼저 확인

- 이번 변경은 생성 DTO·편집 API·인증 전달 방식이 달라집니다. 프론트/백엔드를 같은 배포 회차에 반영하고 재로그인합니다.
- 기존 Render 환경의 AI_MODEL_PLAN override가 이전 모델이면 수정해야 합니다. 코드 기본값만 바꿔도 환경변수 override는 남습니다.
- 현재 로컬 서버 Places 키 호출은 API_KEY_IP_ADDRESS_BLOCKED입니다. 기존 EC2 IP 제한을 Render에 맞게 조정해야 합니다.
- DB 백업 후 Flyway 신규 상태 테이블 적용을 확인합니다. 전체 도메인의 엄격한 마이그레이션 전환은 아직 아닙니다.

## Render 서비스

| 항목 | 값 |
| --- | --- |
| Language / Branch | Docker / main |
| Region / Compute | Singapore / Free |
| Root Directory | backend |
| Dockerfile Path | ./Dockerfile.render |
| Docker Build Context | . |
| Health Check | 새 코드 배포 후 /healthz 지정 (기존 버전은 기본 TCP 검사 유지) |
| Command / Pre-Deploy Command | 빈칸 |

UI가 backend/ 접두사를 자동 표시하면 중복 입력하지 않습니다. 최종 Dockerfile은 backend/Dockerfile.render, 컨텍스트는 backend입니다.

새 버전은 GET / 및 /healthz에 JSON 상태를 반환합니다. 배포 후 /healthz의 200 응답을 확인한 다음 Render Settings의 Health Check Path를 /healthz로 지정합니다. 이전 코드가 실행 중일 때 먼저 설정하지 않습니다. 이 체크는 애플리케이션 응답 여부만 확인하며 DB·Google AI 가용성은 보장하지 않습니다.

## Render 환경변수

| Key | 값 / 설명 |
| --- | --- |
| SPRING_PROFILES_ACTIVE | render (Dockerfile 기본값) |
| DB_JDBC_URL | jdbc:postgresql://NEON_DIRECT_HOST:5432/neondb?sslmode=require |
| DB_USERNAME | Neon Role, 예: neondb_owner |
| DB_PASSWORD | 해당 Role 비밀번호 |
| GOOGLE_CLIENT_ID | Google OAuth 클라이언트 ID |
| GOOGLE_CLIENT_SECRET | 같은 OAuth 클라이언트 Secret |
| JWT_SECRET | 예시가 아닌 충분히 긴 무작위 비밀 값 |
| GOOGLE_PROJECT_ID | Vertex/Agent Platform 사용 프로젝트 |
| GOOGLE_CLOUD_LOCATION | global |
| GOOGLE_CREDENTIALS_URI | file:/etc/secrets/gcp-sa.json |
| GOOGLE_PLACES_API_KEY | 서버 전용 Places API(New) 키, 필수 |
| CORS_ALLOWED_ORIGINS | https://www.compass-travel.site,https://compass-travel.site,https://khsbe-ait.vercel.app |
| OAUTH2_REDIRECT_URI | https://www.compass-travel.site/oauth2/redirect |
| AI_MODEL_PLAN | gemini-4-argon 또는 해당 override 삭제하여 코드 기본값 사용 |
| ADMIN_EMAILS | 관리자 이메일, 쉼표 구분, 필요한 경우 |
| PLAN_QUOTA_PER_DAY | 3 (기본값, 선택) |
| DB_DDL_AUTO | update (현재 전환 기본값, 임의로 validate 변경 금지) |

Secret Files에 gcp-sa.json을 추가하고 서비스 계정 JSON 전체를 등록합니다. 파일은 Git/이미지에 넣지 않습니다. 새 AiConfig는 GOOGLE_CREDENTIALS_URI로 직접 읽습니다. GOOGLE_APPLICATION_CREDENTIALS는 새 경로에서 필수가 아니며 다른 도구에 필요하면 유지해도 됩니다.

GOOGLE_GENAI_VERTEX_AI는 새 어댑터가 읽지 않습니다. 호출 대상은 명시적으로 Vertex generateContent입니다. WEATHER_API_KEY, AI_MODEL_GENERAL, CHAT_QUOTA_PER_DAY, AI_CHAT_HISTORY_WINDOW_MESSAGES, PLACES_VERIFICATION_* 및 REDIS_*는 새 코드에서 사용하지 않습니다. 기존 환경변수는 새 버전이 반영된 뒤 정리할 수 있습니다.

Neon은 직접 연결 주소를 사용하며 JDBC URL에 사용자/비밀번호나 psql 명령을 붙이지 않습니다. JVM 힙은 256MB, 풀은 최대 3개입니다. Free 512MB 환경에서의 실사용 메모리/부하 평가는 별도입니다.

## Google 설정

1. 서비스 계정에 생성 API 권한과 유효한 결제/크레딧이 있는지 확인합니다.
2. Places API(New)를 활성화합니다. 모델 Grounding이 서버 Places 키를 대신하지 않습니다.
3. Render 서비스 화면 → Connect → Outbound의 현재 모든 CIDR 범위를 서버 키의 IP 허용 목록에 등록합니다. 지역 공유 범위이며 전용 고정 IP가 아닙니다.
4. 서버 키의 API 제한은 Places API(New)로 제한합니다. 키를 프론트에 넣거나 IP/API 제한을 모두 제거하지 않습니다.
5. 로컬 테스트가 필요하면 별도 개발 키와 실제 개발 환경 IP 제한을 사용합니다. 로컬 IP는 바뀔 수 있습니다.
6. Google OAuth 승인된 리디렉션 URI: https://khsbe-ait.onrender.com/login/oauth2/code/google

[Render Outbound IP 안내](https://render.com/docs/outbound-ip-addresses), [Maps 키 보안 안내](https://developers.google.com/maps/api-security-best-practices).

## Vercel

Production 환경변수:

```dotenv
VITE_API_BASE_URL=https://khsbe-ait.onrender.com
VITE_GOOGLE_MAPS_API_KEY=<브라우저 전용 키>
```

브라우저 키는 Maps JavaScript API와 Places New 사용 권한, 실제 프론트 사이트의 referrer 제한을 확인합니다. VITE_ 값은 공개되므로 서비스 계정/OAuth Secret을 넣으면 안 됩니다.

compass-frontend/vercel.json의 /api/auth/:path* rewrite를 SPA fallback보다 먼저 유지합니다. 인증 교환/갱신/로그아웃은 프론트와 같은 origin에서 호출하고 Render로 프록시합니다. 이 경로를 API_BASE_URL 직접 호출로 바꾸면 서드파티 쿠키 문제를 다시 만들 수 있습니다. 백엔드 주소가 바뀌면 환경변수와 rewrite를 모두 갱신합니다.

환경변수 변경 뒤 반드시 새 빌드/Redeploy가 필요합니다. 프론트 로그인 완료 주소와 CORS origin에 슬래시/포트가 정확히 일치해야 합니다.

## 반영 순서

1. 로컬 backend test/bootJar, frontend lint/build 확인.
2. DB 백업, Google 키/Render 변수 준비.
3. 사용자가 커밋·push 후 CI 결과 확인.
4. Render는 After CI Checks Pass 또는 수동 배포, Vercel은 Production 배포 확인. 변경 회차 동안 로그인/생성 중간 실패 가능성을 고려합니다.
5. 두 서비스 최신 커밋 확인, 강력 새로고침, 재로그인.
6. 선택형 생성·쿼터·편집·삭제, /api/auth/refresh의 HttpOnly 쿠키 회전 확인.

Live/Ready는 프로세스 기동과 빌드의 증거이며 AI/DB 기능 검증 완료는 아닙니다. 이전 코드로 되돌려도 이미 적용한 Flyway 이력을 삭제하거나 V1 체크섬을 수정하지 않습니다.

무료 호스팅의 휴면·사용 한도와 Vertex/Maps API 비용·크레딧 만료는 별개입니다. 무기한 무중단/무조건 무료를 보장하지 않습니다.
