package com.compass.domain.agent.application;

import static org.junit.jupiter.api.Assertions.*;

import com.compass.domain.agent.dto.PlanRequest;
import com.fasterxml.jackson.databind.*;
import com.google.auth.oauth2.GoogleCredentials;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/** Opt-in only: uses provider credits, never an application database. */
@EnabledIfEnvironmentVariable(named = "RUN_LIVE_AI_TEST", matches = "true")
class LivePlanGenerationTest {
  @Test
  void oneDayGroundedTrip() throws Exception {
    var mapper = new ObjectMapper().findAndRegisterModules();
    GoogleCredentials credentials;
    try (var input = Files.newInputStream(Path.of("../secrets/gcp-sa.json"))) {
      credentials =
          GoogleCredentials.fromStream(input)
              .createScoped(List.of("https://www.googleapis.com/auth/cloud-platform"));
    }
    var catalog = new PlaceCatalog(System.getenv("GOOGLE_PLACES_API_KEY"));
    var gateway =
        new GeminiGateway(
            credentials, mapper, System.getenv("GOOGLE_PROJECT_ID"), "global", "gemini-4-argon");
    var service = new TravelAgentService(gateway, catalog, mapper, new PlanQualityValidator());
    var date = LocalDate.now(ZoneId.of("Asia/Seoul")).plusDays(5);
    var request =
        new PlanRequest(
            UUID.randomUUID(),
            "ChIJNc0j6G3raDURpwhxJHTJXDU",
            date,
            date,
            null,
            null,
            PlanRequest.Companion.FAMILY,
            2,
            1,
            0,
            List.of(PlanRequest.Interest.NATURE, PlanRequest.Interest.FOOD),
            PlanRequest.Pace.RELAXED,
            null,
            List.of(),
            "아이와 해변 산책을 하고 싶어요.");
    long start = System.nanoTime();
    var result = service.generate(request);
    assertEquals(1, result.plan().itineraries().size());
    assertTrue(result.plan().itineraries().get(0).places().size() >= 2);
    assertTrue(
        result.plan().itineraries().get(0).places().stream()
            .allMatch(p -> p.googlePlaceId() != null && !p.verificationUncertain()));
    assertTrue(
        result.candidates().values().stream()
            .anyMatch(c -> c.matchedBy().equals("GOOGLE_MAPS_GROUNDING")));
    System.out.println(
        "LIVE_PLAN_OK days=1 places="
            + result.plan().itineraries().get(0).places().size()
            + " elapsedSeconds="
            + (System.nanoTime() - start) / 1_000_000_000);
  }
}
