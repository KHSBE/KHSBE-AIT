package com.compass.domain.agent.dto;

public record ChatResponse(
        String intent,
        String message,
        Long sessionId,   // ★ 프론트엔드가 방 번호를 알 수 있도록 세션 ID 추가!
        Object planData   // 일정표 데이터가 있을 경우 담아주는 곳
) {
    // 기존 코드 호환성을 위한 오버로딩 (세션ID가 없는 경우)
    public ChatResponse(String intent, String message) {
        this(intent, message, null, null);
    }

    // 일반 대화 응답용 오버로딩
    public ChatResponse(String intent, String message, Long sessionId) {
        this(intent, message, sessionId, null);
    }
}