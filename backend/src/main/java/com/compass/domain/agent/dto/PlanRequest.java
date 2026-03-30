package com.compass.domain.agent.dto;

// 여행 일정 생성 요청 DTO
public record PlanRequest(
        // 기존 세션 ID (null 가능)
        Long sessionId,
        // 사용자 요청 메시지
        String request
) {}