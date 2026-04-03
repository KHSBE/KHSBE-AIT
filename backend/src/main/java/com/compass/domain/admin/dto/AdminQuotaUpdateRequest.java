package com.compass.domain.admin.dto;

import jakarta.validation.constraints.Min;

public record AdminQuotaUpdateRequest(
        @Min(value = 1, message = "planPerDayLimit는 1 이상이어야 합니다.")
        Integer planPerDayLimit,

        @Min(value = 1, message = "chatPerDayLimit는 1 이상이어야 합니다.")
        Integer chatPerDayLimit
) {
}
