# ✈️ VoyageAgent AI: 지능형 맞춤 여행 플래닝 에이전트

> **"사용자의 의도를 읽고, 최적의 경로를 설계하는 AI 여행 에이전트"**
>
> 단순한 일정 기록을 넘어, LLM(Large Language Model)을 기반으로 사용자의 취향과 제약 사항을 반영하여 실시간으로 여행 코스를 제안하고 관리하는 Full-Stack 웹 서비스입니다.

---

## 🚀 Project Overview
* **개발 기간**: 2026.02 ~ 진행 중 (1인 개발)
* **목표**: 복잡한 여행 계획 과정을 AI 에이전트와의 상호작용으로 간소화하며, 대용량 트래픽과 확장성을 고려한 백엔드 아키텍처 구축 실습.

---

## 🛠 Tech Stack

### Backend
- **Language**: Java 21
- **Framework**: Spring Boot 3.4
- **Persistence**: Spring Data JPA, **Querydsl** (동적 쿼리 최적화)
- **AI**: Spring AI (Gemini/OpenAI API 연동)
- **Security**: Spring Security, JWT (Stateless Authentication)
- **Cache**: Redis (API 응답 캐싱 및 세션 관리)

### Frontend
- **Library**: React 18 (Vite)
- **Language**: TypeScript
- **State Management**: TanStack Query (React Query), Zustand
- **Styling**: Tailwind CSS, Shadcn/UI

### Infrastructure & DevOps
- **Database**: MySQL 8.0
- **Container**: Docker, Docker Compose
- **CI/CD**: GitHub Actions
- **Cloud**: AWS EC2, RDS, S3

---

## 🌟 Key Features

### 1. AI Intelligent Planner (Agentic Workflow)
- 사용자의 자연어 요청(예: "부산역 근처 맛집 위주로 1박 2일 짜줘")을 분석하여 데이터베이스 규격에 맞는 JSON 일정표 자동 생성.
- **Prompt Engineering**: 시스템 프롬프트를 통해 AI의 답변 형식을 제어하고 데이터 무결성 확보.

### 2. Interactive Map Integration
- Google Maps API를 연동하여 AI가 추천한 장소를 지도 위에 시각화.
- 장소 간 거리 및 이동 효율을 고려한 경로 최적화 로직 구현.

### 3. Personalization Engine
- 사용자의 과거 여행 스타일 및 선호 카테고리를 가중치로 계산하여 추천 리스트에 반영.
- Redis를 활용하여 최근 검색 및 추천 데이터를 캐싱, 응답 속도 개선.

---

## 🔥 Technical Challenges & Troubleshooting (Portfolio Highlights)

- **N+1 문제 해결**: `Fetch Join`과 `EntityGraph`를 활용하여 연관된 엔티티(Plan - Itinerary - Place) 조회 시 발생하는 성능 저하를 해결.
- **AI 응답 지연 최적화**: LLM 호출 시 발생하는 Latency를 사용자 경험 측면에서 보완하기 위해 **비동기 처리(CompletableFuture)** 및 로딩 UI 최적화.
- **Querydsl 도입**: 검색 조건(지역, 기간, 테마 등)이 늘어남에 따라 복잡해지는 동적 쿼리를 타입 안정성을 보장하며 유지보수 가능하도록 개선.

---

## 📐 System Architecture (준비 중)
*이곳에 아키텍처 다이어그램 이미지를 추가할 예정입니다.*

---

## 🗄️ Database Schema (ERD 초안)
- `User`: 회원 정보 및 인증 데이터
- `TravelPlan`: 여행의 전체적인 메타데이터 (제목, 기간)
- `Itinerary`: 날짜별 일정 그룹화
- `Place`: 세부 장소 정보 (좌표, 주소, 순서)
- `AgentLog`: AI 에이전트와의 대화 및 추천 이력 관리