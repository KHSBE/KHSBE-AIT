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
            Double latitude,
            Double longitude,
            @Size(max = 2000) String memo
    ) {
    }
}
