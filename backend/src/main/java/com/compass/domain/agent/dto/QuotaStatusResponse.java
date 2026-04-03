package com.compass.domain.agent.dto;

public record QuotaStatusResponse(
        int planPerDayLimit,
        int planPerDayUsed,
        int planPerDayRemaining,
        int chatPerSessionLimit,
        int chatPerSessionUsed,
        int chatPerSessionRemaining
) {
}
