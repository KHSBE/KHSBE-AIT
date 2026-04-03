package com.compass.domain.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ChatRequest(
        @Positive(message = "sessionId must be a positive number.")
        Long sessionId,

        @NotBlank(message = "message is required.")
        @Size(max = 2000, message = "message must be 2000 characters or fewer.")
        String message
) {
}
