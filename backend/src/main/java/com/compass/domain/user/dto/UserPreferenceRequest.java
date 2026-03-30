package com.compass.domain.user.dto;



public record UserPreferenceRequest(
        String preferredCategory,
        String dietRestriction,
        String travelPace
) {}