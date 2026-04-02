package com.compass.domain.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record PlanRequest(
        @Positive(message = "sessionId는 양수여야 합니다.")
        Long sessionId,

        @NotBlank(message = "여행 요청 내용은 비어 있을 수 없습니다.")
        @Size(max = 2000, message = "여행 요청 내용은 2000자 이하여야 합니다.")
        String request
) {}
