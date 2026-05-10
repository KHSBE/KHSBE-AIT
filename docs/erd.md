# ERD

현재 JPA 엔티티 기준의 주요 테이블 구조입니다.

```mermaid
erDiagram
    USERS ||--o{ TRAVEL_PLANS : owns
    USERS ||--o{ CHAT_SESSIONS : owns
    USERS ||--|| USER_PREFERENCE : has
    USERS ||--o| USER_QUOTA_SETTINGS : overrides
    USERS ||--o{ USER_QUOTA_DAILY_RESETS : resets

    TRAVEL_PLANS ||--o{ ITINERARIES : contains
    ITINERARIES ||--o{ PLACE : contains
    TRAVEL_PLANS ||--o{ CHAT_SESSIONS : linked
    CHAT_SESSIONS ||--o{ CHAT_MESSAGES : contains

    USERS {
        bigint id PK
        varchar email UK
        varchar password
        varchar nickname
        varchar role
        datetime created_at
        datetime updated_at
    }

    USER_PREFERENCE {
        bigint id PK
        bigint user_id FK
        varchar preferred_category
        varchar diet_restriction
        varchar travel_pace
    }

    TRAVEL_PLANS {
        bigint id PK
        bigint user_id FK
        varchar title
        varchar region
        varchar departure
        varchar companion
        varchar style
        varchar accommodation
        varchar accommodation_place_id
        double accommodation_latitude
        double accommodation_longitude
        varchar accommodation_address
        double accommodation_confidence
        boolean accommodation_verification_uncertain
        date start_date
        date end_date
        datetime created_at
        datetime updated_at
    }

    ITINERARIES {
        bigint id PK
        bigint travel_plan_id FK
        int day_number
        date travel_date
        varchar theme
    }

    PLACE {
        bigint id PK
        bigint itinerary_id FK
        varchar place_name
        varchar google_place_id
        varchar start_time
        int duration_minutes
        varchar category
        double latitude
        double longitude
        int visit_order
        double confidence
        varchar matched_by
        boolean verification_uncertain
        text memo
    }

    CHAT_SESSIONS {
        bigint id PK
        bigint user_id FK
        bigint plan_id FK
        varchar session_title
        datetime created_at
        datetime updated_at
    }

    CHAT_MESSAGES {
        bigint id PK
        bigint session_id FK
        varchar sender_role
        text content
        datetime sent_at
    }

    USER_QUOTA_SETTINGS {
        bigint id PK
        bigint user_id UK
        int plan_per_day_limit
        int chat_per_day_limit
        datetime created_at
        datetime updated_at
    }

    USER_QUOTA_DAILY_RESETS {
        bigint id PK
        bigint user_id
        date quota_date
        int plan_used_baseline
        int chat_used_baseline
        datetime created_at
        datetime updated_at
    }
```

## 핵심 설계 메모

- `users.role`은 `USER`, `ADMIN` 값을 가지며 관리자 페이지 접근 권한에 사용됩니다.
- `travel_plans.accommodation_*` 필드는 숙소도 장소처럼 검증하고 지도에 표시하기 위한 필드입니다.
- `place.confidence`, `place.matched_by`, `place.verification_uncertain`은 Places 검증 결과를 저장합니다.
- `user_quota_settings`는 사용자별 쿼터 override를 저장합니다.
- `user_quota_daily_resets`는 관리자가 당일 사용량을 기본값 기준으로 리필할 때 baseline을 저장합니다.

## 주의 사항

현재 프로젝트는 Hibernate DDL 자동 반영에 의존하는 부분이 있습니다. 운영 안정성을 높이려면 다음 단계로 Flyway를 도입해 스키마 변경 이력을 명시적으로 관리하는 것이 좋습니다.
