package com.compass.domain.agent.application;

import com.compass.domain.agent.dto.AiPlanResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class PlacesVerificationService {

    private static final String FIELD_MASK = "places.id,places.displayName,places.location";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final String apiKey;
    private final String languageCode;
    private final int maxPlacesPerPlan;

    public PlacesVerificationService(
            ObjectMapper objectMapper,
            @Value("${app.places.verification.enabled:true}") boolean enabled,
            @Value("${app.places.api-key:}") String apiKey,
            @Value("${app.places.language-code:ko}") String languageCode,
            @Value("${app.places.verification.max-places-per-plan:20}") int maxPlacesPerPlan
    ) {
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.apiKey = apiKey;
        this.languageCode = languageCode;
        this.maxPlacesPerPlan = maxPlacesPerPlan;
        this.restClient = RestClient.builder()
                .baseUrl("https://places.googleapis.com")
                .build();
    }

    public AiPlanResponse verifyPlan(AiPlanResponse plan) {
        if (plan == null || plan.itineraries() == null) {
            return plan;
        }
        if (!enabled || apiKey == null || apiKey.isBlank()) {
            log.info("[PLACES-VERIFY] skipped. enabled={}, apiKeyPresent={}", enabled, apiKey != null && !apiKey.isBlank());
            return plan;
        }

        int verifiedCount = 0;
        List<AiPlanResponse.DayItinerary> normalizedDays = new ArrayList<>();

        for (AiPlanResponse.DayItinerary day : plan.itineraries()) {
            if (day == null || day.places() == null) {
                normalizedDays.add(day);
                continue;
            }

            List<AiPlanResponse.PlaceDetail> normalizedPlaces = new ArrayList<>();
            for (AiPlanResponse.PlaceDetail place : day.places()) {
                if (place == null) {
                    continue;
                }

                if (verifiedCount >= Math.max(1, maxPlacesPerPlan)) {
                    normalizedPlaces.add(place);
                    continue;
                }

                normalizedPlaces.add(verifyPlace(place, plan.region()));
                verifiedCount++;
            }

            normalizedDays.add(new AiPlanResponse.DayItinerary(day.day(), day.theme(), normalizedPlaces));
        }

        log.info("[PLACES-VERIFY] completed. verifiedCount={}", verifiedCount);
        return new AiPlanResponse(
                plan.title(),
                plan.region(),
                plan.accommodation(),
                plan.startDate(),
                plan.endDate(),
                normalizedDays
        );
    }

    private AiPlanResponse.PlaceDetail verifyPlace(AiPlanResponse.PlaceDetail place, String region) {
        if (place.name() == null || place.name().isBlank()) {
            return place;
        }

        String textQuery = buildQuery(region, place.name());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("textQuery", textQuery);
        body.put("languageCode", languageCode);

        try {
            String response = restClient.post()
                    .uri("/v1/places:searchText")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Goog-Api-Key", apiKey)
                    .header("X-Goog-FieldMask", FIELD_MASK)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            if (response == null || response.isBlank()) {
                return place;
            }

            JsonNode root = objectMapper.readTree(response);
            JsonNode places = root.path("places");
            if (!places.isArray() || places.isEmpty()) {
                return place;
            }

            JsonNode first = places.get(0);
            String normalizedName = text(first.path("displayName"), "text", place.name());
            String googlePlaceId = text(first, "id", place.googlePlaceId());
            Double lat = number(first.path("location"), "latitude", place.lat());
            Double lng = number(first.path("location"), "longitude", place.lng());

            return new AiPlanResponse.PlaceDetail(
                    normalizedName,
                    place.memo(),
                    lat,
                    lng,
                    googlePlaceId
            );
        } catch (Exception e) {
            log.warn("[PLACES-VERIFY] failed for query='{}': {}", textQuery, e.getMessage());
            return place;
        }
    }

    private String buildQuery(String region, String placeName) {
        if (region == null || region.isBlank()) {
            return placeName.trim();
        }
        return (region + " " + placeName).trim();
    }

    private String text(JsonNode node, String field, String fallback) {
        JsonNode value = node.path(field);
        if (value.isTextual() && !value.asText().isBlank()) {
            return value.asText();
        }
        return fallback;
    }

    private Double number(JsonNode node, String field, Double fallback) {
        JsonNode value = node.path(field);
        if (value.isNumber()) {
            return value.asDouble();
        }
        return fallback;
    }
}
