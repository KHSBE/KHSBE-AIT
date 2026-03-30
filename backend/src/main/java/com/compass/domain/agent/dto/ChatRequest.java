package com.compass.domain.agent.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChatRequest {
    private Long sessionId; // 기존 세션 ID (null이면 새 세션 생성)
    private String message;  // 유저의 발화 내용
}