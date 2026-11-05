package com.compass.domain.agent.application;

import com.compass.global.error.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@lombok.extern.slf4j.Slf4j
@Service
public class PlaceCatalog {
  private static final String FIELDS =
      "id,displayName,formattedAddress,location,viewport,addressComponents,types,businessStatus,regularOpeningHours,currentOpeningHours,utcOffsetMinutes,googleMapsUri";
  private final RestClient client;
  private final String apiKey;

  public PlaceCatalog(@Value("${app.places.api-key:}") String apiKey) {
    this.apiKey = apiKey;
    var factory =
        new JdkClientHttpRequestFactory(
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
    factory.setReadTimeout(Duration.ofSeconds(12));
    this.client =
        RestClient.builder()
            .baseUrl("https://places.googleapis.com")
            .requestFactory(factory)
            .build();
  }

  public PlaceInfo details(String id) {
    id = normalizeId(id);
    if (apiKey.isBlank())
      throw new BusinessException("장소 검증 API 설정이 필요합니다.", HttpStatus.SERVICE_UNAVAILABLE);
    try {
      JsonNode response =
          client
              .get()
              .uri("/v1/places/{id}?languageCode=ko", id)
              .header("X-Goog-Api-Key", apiKey)
              .header("X-Goog-FieldMask", FIELDS)
              .retrieve()
              .body(JsonNode.class);
      if (response == null || !response.has("location"))
        throw new IllegalStateException("Missing location");
      return parse(response);
    } catch (BusinessException e) {
      throw e;
    } catch (Exception e) {
      logFailure(e);
      throw new BusinessException(
          "장소 정보를 확인하지 못했습니다. 다른 장소를 선택하거나 잠시 후 다시 시도해 주세요.", HttpStatus.BAD_GATEWAY);
    }
  }

  public List<PlaceInfo> search(String query, PlaceInfo destination, int size) {
    if (apiKey.isBlank())
      throw new BusinessException("장소 검증 API 설정이 필요합니다.", HttpStatus.SERVICE_UNAVAILABLE);
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("textQuery", query);
    body.put("languageCode", "ko");
    body.put("pageSize", Math.min(20, Math.max(1, size)));
    body.put(
        "locationRestriction",
        Map.of(
            "rectangle",
            Map.of(
                "low", Map.of("latitude", destination.south(), "longitude", destination.west()),
                "high", Map.of("latitude", destination.north(), "longitude", destination.east()))));
    try {
      JsonNode response =
          client
              .post()
              .uri("/v1/places:searchText")
              .header("X-Goog-Api-Key", apiKey)
              .header(
                  "X-Goog-FieldMask",
                  Arrays.stream(FIELDS.split(","))
                      .map(s -> "places." + s)
                      .collect(java.util.stream.Collectors.joining(",")))
              .contentType(MediaType.APPLICATION_JSON)
              .body(body)
              .retrieve()
              .body(JsonNode.class);
      List<PlaceInfo> result = new ArrayList<>();
      if (response != null) for (JsonNode node : response.path("places")) result.add(parse(node));
      return result;
    } catch (Exception e) {
      logFailure(e);
      throw new BusinessException(
          "여행 지역의 장소 후보를 찾지 못했습니다. 잠시 후 다시 시도해 주세요.", HttpStatus.BAD_GATEWAY);
    }
  }

  private void logFailure(Exception error) {
    if (error instanceof org.springframework.web.client.RestClientResponseException response)
      log.warn("event=places_failed http={}", response.getStatusCode().value());
    else log.warn("event=places_failed type={}", error.getClass().getSimpleName());
  }

  private PlaceInfo parse(JsonNode n) {
    String country = "";
    for (JsonNode a : n.path("addressComponents")) {
      for (JsonNode type : a.path("types"))
        if ("country".equals(type.asText())) country = a.path("shortText").asText();
    }
    List<String> types = new ArrayList<>();
    n.path("types").forEach(t -> types.add(t.asText()));
    double lat = n.path("location").path("latitude").asDouble();
    double lng = n.path("location").path("longitude").asDouble();
    JsonNode viewport = n.path("viewport");
    return new PlaceInfo(
        n.path("id").asText(),
        n.path("displayName").path("text").asText(),
        n.path("formattedAddress").asText(),
        lat,
        lng,
        country,
        List.copyOf(types),
        n.path("businessStatus").asText(),
        viewport.path("low").path("latitude").asDouble(lat - .15),
        viewport.path("low").path("longitude").asDouble(lng - .15),
        viewport.path("high").path("latitude").asDouble(lat + .15),
        viewport.path("high").path("longitude").asDouble(lng + .15),
        n.path("regularOpeningHours"),
        n.path("currentOpeningHours"),
        n.path("googleMapsUri").asText());
  }

  public static String normalizeId(String raw) {
    String id = raw == null ? "" : raw.replaceFirst("^places/", "");
    if (!id.matches("[A-Za-z0-9_-]{5,255}"))
      throw new BusinessException("올바른 장소를 선택해 주세요.", HttpStatus.BAD_REQUEST);
    return id;
  }

  public record PlaceInfo(
      String id,
      String name,
      String address,
      double lat,
      double lng,
      String country,
      List<String> types,
      String businessStatus,
      double south,
      double west,
      double north,
      double east,
      JsonNode regularHours,
      JsonNode currentHours,
      String mapsUri) {
    public boolean contains(PlaceInfo p) {
      boolean longitude =
          west <= east ? p.lng >= west && p.lng <= east : p.lng >= west || p.lng <= east;
      return !country.isBlank()
          && country.equals(p.country)
          && p.lat >= south
          && p.lat <= north
          && longitude;
    }

    public boolean available() {
      return !businessStatus.startsWith("CLOSED") && !businessStatus.equals("FUTURE_OPENING");
    }

    public boolean city() {
      return types.stream()
          .anyMatch(
              t ->
                  Set.of(
                          "locality",
                          "administrative_area_level_1",
                          "administrative_area_level_2",
                          "postal_town")
                      .contains(t));
    }
  }
}
