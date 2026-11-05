package com.compass.domain.agent.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record PlanRequest(
    @NotNull UUID requestId,
    @NotBlank @Size(max = 255) String destinationPlaceId,
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate,
    LocalTime arrivalTime,
    LocalTime departureTime,
    @NotNull Companion companion,
    @Min(1) @Max(20) int adults,
    @Min(0) @Max(10) int children,
    @Min(0) @Max(20) int seniors,
    @NotEmpty @Size(max = 5) List<@NotNull Interest> interests,
    @NotNull Pace pace,
    @Size(max = 255) String accommodationPlaceId,
    @NotNull @Size(max = 5) List<@NotBlank @Size(max = 255) String> requiredPlaceIds,
    @Size(max = 500) String additionalNotes) {
  public enum Companion {
    SOLO,
    COUPLE,
    FRIENDS,
    FAMILY
  }

  public enum Interest {
    FOOD,
    CAFE,
    NATURE,
    CULTURE,
    SHOPPING,
    EXPERIENCE
  }

  public enum Pace {
    RELAXED,
    BALANCED,
    ACTIVE
  }
}
