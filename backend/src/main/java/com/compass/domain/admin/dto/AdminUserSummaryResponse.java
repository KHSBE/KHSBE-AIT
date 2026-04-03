package com.compass.domain.admin.dto;

public record AdminUserSummaryResponse(
        Long userId,
        String email,
        String nickname
) {
}
