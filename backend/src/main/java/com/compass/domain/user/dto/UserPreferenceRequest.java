package com.compass.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserPreferenceRequest(
        @NotBlank(message = "선호 카테고리는 필수입니다.")
        @Size(max = 50, message = "선호 카테고리는 50자 이하여야 합니다.")
        String preferredCategory,

        @NotBlank(message = "식단 제한 정보는 필수입니다.")
        @Size(max = 50, message = "식단 제한 정보는 50자 이하여야 합니다.")
        String dietRestriction,

        @NotBlank(message = "여행 페이스는 필수입니다.")
        @Size(max = 30, message = "여행 페이스는 30자 이하여야 합니다.")
        String travelPace
) {}
