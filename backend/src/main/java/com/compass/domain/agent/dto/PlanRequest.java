package com.compass.domain.agent.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PlanRequest {
    @NotBlank(message = "여행 요청 사항을 입력해주세요.")
    private String request; // 예: "부산 1박 2일 맛집 투어 코스 짜줘"
}