# Compass

한국인 여행자를 위한 선택형 AI 여행 플래너입니다. 여행지·날짜·동행·관심사를 고르면 Google Maps 기반으로 실제 장소 후보를 찾고, Places API(New)로 확인한 후보 안에서 일정 초안을 만듭니다.

> 이번 선택형 전환은 로컬 구현입니다. 배포된 서비스와 동작이 다를 수 있습니다. 실제 일정 생성의 전체 연동 검증은 Places 키의 IP 제한 조정 후 필요합니다.

## 핵심 흐름

`5단계 여행 조건 선택 → Maps Grounding → Places 확인 → 일정 JSON 생성 → 품질 검사/1회 재생성 → 일정·사용량 함께 저장`

- 도시·국가 불일치, 폐업 상태, 후보에 없는 장소 ID, 중복 장소를 차단합니다.
- 여행 강도별 장소 수, 방문/체류 시각, 시간 중복, 제공된 영업시간과 필수 장소를 검사합니다.
- 실패 시 사용량을 확정하지 않습니다. 요청 ID 재전송은 기존 결과를 반환하며 사용량은 일정 삭제와 분리합니다.
- 장소 교체는 서버에서 다시 확인하고 모든 날짜를 하나의 트랜잭션으로 저장합니다.
- 일반 채팅·의도 감지·날씨 도구는 제거했습니다. 숙소는 사용자가 선택한 시설만 확인하며 임의 추천하지 않습니다.

## 기술

| 영역 | 구성 |
| --- | --- |
| 프론트 | React, Vite, Pretendard, Google Maps JavaScript Places New |
| 백엔드 | Java 17, Spring Boot, Security, JPA, Querydsl |
| AI | Google Cloud `generateContent` REST, 기본 `gemini-4-argon`, Maps Places-only Grounding |
| 인증 | Google OAuth2 → 90초 일회용 코드 → Access JWT + HttpOnly Refresh Cookie |
| 데이터 | Neon PostgreSQL, 생성 요청/일일 사용량/해시 인증 토큰 저장 |
| 스키마 | 신규 상태 테이블 Flyway V1; 기존 도메인은 아직 Hibernate update 병행 |
| 배포/검증 | Vercel + Render, GitHub Actions 백엔드 테스트·프론트 lint/build |

Spring AI·Redis·pgvector 런타임 의존성 및 Places 응답의 24시간 캐시는 제거했습니다. 지도 연결선은 방문 순서이지 실제 길찾기 경로가 아닙니다.

## API

| Method | Path | 설명 |
| --- | --- | --- |
| POST | `/api/auth/exchange` | 로그인 일회용 코드를 세션으로 교환 |
| POST | `/api/auth/refresh` | 쿠키 회전과 Access 재발급 |
| POST | `/api/auth/logout` | Refresh 계열 폐기 |
| POST | `/api/agent/plan` | 구조화된 조건과 requestId로 생성 |
| GET | `/api/agent/quota` | 오늘 사용 가능 횟수 |
| GET | `/api/plans` | 내 일정 |
| GET / DELETE | `/api/plans/{id}` | 상세 / 삭제 |
| PUT | `/api/plans/{id}/places` | 모든 날짜의 장소 일괄 수정 |
| GET | `/api/admin/me`, `/api/admin/users` | 관리자 확인 / 사용자 검색 |
| GET / PUT / DELETE | `/api/admin/users/{id}/quota` | 쿼터 조회 / 변경 / 기본값으로 충전 |

## 실행과 문서

- [배포 가이드](docs/RENDER_DEPLOYMENT.md): 환경변수, Google API 키 제한, 인증 프록시.
- [개발자 가이드](docs/개발자_실행가이드.md): Docker 없이 테스트하기.
- [전환 내역과 검증 체크리스트](docs/선택형_일정_전환.md).
- [아키텍처](docs/아키텍처.md), [ERD](docs/erd.md), [운영 런북](docs/서버_운영_런북.md), [문서 인덱스](docs/README.md).

프론트: [Compass](https://www.compass-travel.site) / 백엔드: [Render API](https://khsbe-ait.onrender.com).

## 한계

장소의 실재 확인이 여행 전체의 실행 가능성을 보장하지는 않습니다. 영업시간 누락·미래의 임시 휴무·예약/매진·실제 이동시간은 확정하지 못합니다. 최소 20분 간격은 계획 여유이며 경로 계산값이 아닙니다. 통계적 정확도나 방문 가능 확률은 표시하지 않습니다.

실서비스 전에는 PostgreSQL 통합/부하 테스트, 실제 로그인 쿠키 프록시 검증, 여러 도시의 일정 품질 평가, Google 콘텐츠 보관·표시 정책 검토가 필요합니다. AI 부분 수정과 사진 중심 상세 화면은 후속 작업입니다.