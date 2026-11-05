package com.compass.domain.agent.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.compass.domain.agent.dto.PlanRequest;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.MissingNode;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class GenerationPipelineTest {
  @Test
  void unknownIdIsRejectedAndRepairedUsingVerifiedCandidates() throws Exception {
    var mapper = new ObjectMapper().findAndRegisterModules();
    var gateway = mock(GeminiGateway.class);
    var catalog = mock(PlaceCatalog.class);
    var service = new TravelAgentService(gateway, catalog, mapper, new PlanQualityValidator());
    var date = LocalDate.now(ZoneId.of("Asia/Seoul")).plusDays(20);
    var request =
        new PlanRequest(
            UUID.randomUUID(),
            "city-123",
            date,
            date,
            null,
            null,
            PlanRequest.Companion.SOLO,
            1,
            0,
            0,
            List.of(PlanRequest.Interest.FOOD),
            PlanRequest.Pace.RELAXED,
            null,
            List.of(),
            "");
    when(catalog.details("city-123")).thenReturn(place("city-123", List.of("locality")));
    when(catalog.details("place-111"))
        .thenReturn(place("place-111", List.of("tourist_attraction")));
    when(catalog.details("place-222")).thenReturn(place("place-222", List.of("restaurant")));
    var grounding =
        mapper.readTree(
            "{\"candidates\":[{\"groundingMetadata\":{\"groundingChunks\":[{\"maps\":{\"placeId\":\"places/place-111\",\"title\":\"Source"
                + " title\",\"uri\":\"https://maps.google.com/?cid=123\"}},{\"maps\":{\"placeId\":\"place-222\",\"title\":\"Second\",\"uri\":\"https://maps.google.com/?cid=456\"}}]}}]}");
    when(gateway.generate(anyString(), isNull(), eq(true), anyDouble(), anyDouble(), anyInt()))
        .thenReturn(grounding);
    String valid =
        "{\"title\":\"여행\",\"days\":[{\"day\":1,\"theme\":\"산책\",\"places\":[{\"placeId\":\"place-111\",\"startTime\":\"10:00\",\"durationMinutes\":60,\"category\":\"관광\",\"memo\":\"추천"
            + " 이유\"},{\"placeId\":\"place-222\",\"startTime\":\"12:00\",\"durationMinutes\":60,\"category\":\"식사\",\"memo\":\"점심\"}]}]}";
    when(gateway.generate(anyString(), notNull(), eq(false), anyDouble(), anyDouble(), anyInt()))
        .thenReturn(
            response(mapper, valid.replace("place-111", "invented-id")), response(mapper, valid));
    when(gateway.text(any())).thenCallRealMethod();
    var result = service.generate(request);
    assertEquals("verified-place-111", result.plan().itineraries().get(0).places().get(0).name());
    assertEquals("Source title", result.candidates().get("place-111").sourceTitle());
    assertNull(result.plan().itineraries().get(0).places().get(0).confidence());
    var schedulePrompts = org.mockito.ArgumentCaptor.forClass(String.class);
    verify(gateway, times(2))
        .generate(
            schedulePrompts.capture(), notNull(), eq(false), anyDouble(), anyDouble(), anyInt());
    assertTrue(
        schedulePrompts.getAllValues().stream()
            .noneMatch(p -> p.contains("UNNECESSARY_WEEKDAY_DESCRIPTION")));
    verify(catalog, never()).search(anyString(), any(), anyInt());
  }

  PlaceCatalog.PlaceInfo place(String id, List<String> types) {
    return new PlaceCatalog.PlaceInfo(
        id,
        "verified-" + id,
        "address",
        1,
        1,
        "KR",
        types,
        "OPERATIONAL",
        0,
        0,
        10,
        10,
        new ObjectMapper()
            .valueToTree(
                Map.of(
                    "periods",
                    List.of(),
                    "weekdayDescriptions",
                    List.of("UNNECESSARY_WEEKDAY_DESCRIPTION"))),
        MissingNode.getInstance(),
        "https://maps.google.com/?cid=123");
  }

  JsonNode response(ObjectMapper mapper, String text) {
    return mapper.valueToTree(
        Map.of(
            "candidates",
            List.of(Map.of("content", Map.of("parts", List.of(Map.of("text", text)))))));
  }

  @Test
  void transientMapsFailureFallsBackToVerifiedPlacesWithoutInventedGrounding() throws Exception {
    var mapper = new ObjectMapper().findAndRegisterModules();
    var gateway = mock(GeminiGateway.class);
    var catalog = mock(PlaceCatalog.class);
    var service = new TravelAgentService(gateway, catalog, mapper, new PlanQualityValidator());
    var date = LocalDate.now(ZoneId.of("Asia/Seoul")).plusDays(20);
    var request =
        new PlanRequest(
            UUID.randomUUID(),
            "city-123",
            date,
            date,
            null,
            null,
            PlanRequest.Companion.SOLO,
            1,
            0,
            0,
            List.of(PlanRequest.Interest.FOOD),
            PlanRequest.Pace.RELAXED,
            null,
            List.of(),
            "");
    when(catalog.details("city-123")).thenReturn(place("city-123", List.of("locality")));
    when(gateway.generate(anyString(), isNull(), eq(true), anyDouble(), anyDouble(), anyInt()))
        .thenThrow(
            new AiUnavailableException(
                "busy", org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE));
    when(catalog.search(anyString(), any(), anyInt()))
        .thenReturn(
            List.of(
                place("place-111", List.of("tourist_attraction")),
                place("place-222", List.of("restaurant"))));
    String valid =
        "{\"title\":\"여행\",\"days\":[{\"day\":1,\"theme\":\"산책\",\"places\":[{\"placeId\":\"place-111\",\"startTime\":\"10:00\",\"durationMinutes\":60,\"category\":\"관광\",\"memo\":\"산책\"},{\"placeId\":\"place-222\",\"startTime\":\"12:00\",\"durationMinutes\":60,\"category\":\"식사\",\"memo\":\"점심\"}]}]}";
    when(gateway.generate(anyString(), notNull(), eq(false), anyDouble(), anyDouble(), anyInt()))
        .thenReturn(response(mapper, valid));
    when(gateway.text(any())).thenCallRealMethod();
    var result = service.generate(request);
    assertEquals(2, result.plan().itineraries().get(0).places().size());
    assertTrue(
        result.candidates().values().stream().allMatch(c -> c.matchedBy().equals("PLACES_API")));
    assertTrue(
        result.plan().itineraries().get(0).places().stream()
            .allMatch(p -> !p.verificationUncertain()));
    verify(catalog, times(3)).search(anyString(), any(), anyInt());
  }

  @Test
  void authenticationAndHardQuotaFailuresDoNotSilentlyFallback() {
    var gateway = mock(GeminiGateway.class);
    var catalog = mock(PlaceCatalog.class);
    var service =
        new TravelAgentService(
            gateway,
            catalog,
            new ObjectMapper().findAndRegisterModules(),
            new PlanQualityValidator());
    var date = LocalDate.now(ZoneId.of("Asia/Seoul")).plusDays(20);
    var request =
        new PlanRequest(
            UUID.randomUUID(),
            "city-123",
            date,
            date,
            null,
            null,
            PlanRequest.Companion.SOLO,
            1,
            0,
            0,
            List.of(PlanRequest.Interest.FOOD),
            PlanRequest.Pace.RELAXED,
            null,
            List.of(),
            "");
    when(catalog.details("city-123")).thenReturn(place("city-123", List.of("locality")));
    when(gateway.generate(anyString(), isNull(), eq(true), anyDouble(), anyDouble(), anyInt()))
        .thenThrow(
            new com.compass.global.error.BusinessException(
                "quota or auth", org.springframework.http.HttpStatus.BAD_GATEWAY));
    assertThrows(com.compass.global.error.BusinessException.class, () -> service.generate(request));
    verify(catalog, never()).search(anyString(), any(), anyInt());
  }
}
