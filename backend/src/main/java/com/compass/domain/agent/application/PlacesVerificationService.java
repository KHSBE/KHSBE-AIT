package com.compass.domain.agent.application;

/** Persisted accommodation contract; live lookup is handled by PlaceCatalog. */
public final class PlacesVerificationService {
  private PlacesVerificationService() {}

  public record AccommodationVerification(
      String name,
      String googlePlaceId,
      Double lat,
      Double lng,
      String address,
      Double confidence,
      Boolean verificationUncertain) {}
}
