package com.compass.domain.plan.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateItineraryPlacesRequest(
        @NotNull @Valid List<PlaceItem> places
) {
    public record PlaceItem(
            @NotBlank @Size(max = 120) String placeName,
            @Size(max = 255) String googlePlaceId,
            @Size(max = 20) String startTime,
            Integer durationMinutes,
            @Size(max = 40) String category,
            Double latitude,
            Double longitude,
            @Size(max = 2000) String memo,
            Double confidence,
            @Size(max = 80) String matchedBy,
            Boolean verificationUncertain
    ) {
        public PlaceItem(
                String placeName,
                String googlePlaceId,
                Double latitude,
                Double longitude,
                String memo
        ) {
            this(placeName, googlePlaceId, null, null, null, latitude, longitude, memo, null, null, false);
        }
    }
}
