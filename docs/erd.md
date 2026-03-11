erDiagram
USERS ||--o| USER_PREFERENCES : "defines"
USERS ||--o{ TRAVEL_PLANS : "owns"
USERS ||--o{ CHAT_SESSIONS : "starts"

    TRAVEL_PLANS ||--o{ ITINERARIES : "structured_by"
    TRAVEL_PLANS ||--o{ CHAT_SESSIONS : "associated_with"
    
    ITINERARIES ||--o{ PLACES : "contains"
    
    CHAT_SESSIONS ||--o{ CHAT_MESSAGES : "recorded_in"

    USERS {
        Long id PK "Auto Increment"
        String email UK "로그인 ID"
        String password "암호화된 비번"
        String nickname "사용자 이름"
        String role "ROLE_USER, ROLE_ADMIN"
        DateTime created_at
        DateTime updated_at
    }

    USER_PREFERENCES {
        Long id PK
        Long user_id FK "Users 참조"
        String preferred_category "식도락, 액티비티, 휴양 등 (JSON or CSV)"
        String diet_restriction "다이어트/알레르기 정보 (AI 추천 가중치)"
        Integer daily_budget "선호 일일 예산"
        String travel_pace "느긋한, 타이트한"
    }

    TRAVEL_PLANS {
        Long id PK
        Long user_id FK
        String title "여행 제목"
        String region "여행지 (도시/국가)"
        LocalDate start_date
        LocalDate end_date
        Boolean is_public "공유 여부"
        String status "PLANNING, COMPLETED, DELETED"
        DateTime created_at
    }

    ITINERARIES {
        Long id PK
        Long plan_id FK "TravelPlans 참조"
        Integer day_number "n일차"
        LocalDate travel_date "실제 여행 날짜"
        String theme "해당 일차의 테마 (예: 힐링, 맛집탐방)"
    }

    PLACES {
        Long id PK
        Long itinerary_id FK "Itineraries 참조"
        String place_name "장소명"
        String google_place_id "API 연동용 고유 ID"
        String address
        Double latitude
        Double longitude
        Integer visit_order "방문 순서 (정렬용)"
        LocalTime arrival_time "예상 도착 시간"
        Integer stay_duration "예상 체류 시간(분)"
        String memo "에이전트의 한줄 팁"
    }

    CHAT_SESSIONS {
        Long id PK
        Long user_id FK
        Long plan_id FK "null 가능 (계획 생성 전 대화)"
        String session_title "대화 요약 제목"
        DateTime last_message_at
    }

    CHAT_MESSAGES {
        Long id PK
        Long session_id FK "ChatSessions 참조"
        String sender_role "USER, ASSISTANT"
        Text message_content "대화 텍스트"
        DateTime sent_at
    }