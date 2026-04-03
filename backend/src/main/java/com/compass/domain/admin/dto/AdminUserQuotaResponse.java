package com.compass.domain.admin.dto;

public record AdminUserQuotaResponse(
        Long userId,
        String email,
        String nickname,
        int defaultPlanPerDayLimit,
        Integer overridePlanPerDayLimit,
        int effectivePlanPerDayLimit,
        int planPerDayUsed,
        int planPerDayRemaining,
        int defaultChatPerDayLimit,
        Integer overrideChatPerDayLimit,
        int effectiveChatPerDayLimit,
        int chatPerDayUsed,
        int chatPerDayRemaining
) {
}
