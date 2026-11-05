package com.compass.domain.plan.dto;

import com.compass.domain.plan.dto.UpdateItineraryPlacesRequest.PlaceItem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record UpdatePlanRequest(@NotEmpty @Size(max = 30) List<@Valid Day> days) {
  public record Day(
      @NotNull @Positive Long itineraryId, @NotNull @Size(max = 6) List<@Valid PlaceItem> places) {}
}
