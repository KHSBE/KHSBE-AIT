package com.compass.domain.agent.application;

import com.compass.domain.agent.dto.AiPlanResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Service
@Slf4j
public class PlacesVerificationService {

    private static final String FIELD_MASK = "places.id,places.displayName,places.location,places.formattedAddress,places.primaryType,places.types,places.rating,places.userRatingCount";
    private static final Pattern NON_WORD = Pattern.compile("[^\\p{L}\\p{N}\\s]");
    private static final Pattern SPACES = Pattern.compile("\\s+");

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final String apiKey;
    private final String languageCode;
    private final int maxPlacesPerPlan;
    private final int minScore;
    private final int candidatesPerQuery;
    private final long cacheTtlMillis;

    private final Map<String, CacheEntry> searchCache = new ConcurrentHashMap<>();

    public PlacesVerificationService(
            ObjectMapper objectMapper,
            @Value("${app.places.verification.enabled:true}") boolean enabled,
            @Value("${app.places.api-key:}") String apiKey,
            @Value("${app.places.language-code:ko}") String languageCode,
            @Value("${app.places.verification.max-places-per-plan:20}") int maxPlacesPerPlan,
            @Value("${app.places.verification.min-score:45}") int minScore,
            @Value("${app.places.verification.candidates-per-query:5}") int candidatesPerQuery,
            @Value("${app.places.verification.cache-ttl-hours:24}") long cacheTtlHours
    ) {
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.apiKey = apiKey;
        this.languageCode = languageCode;
        this.maxPlacesPerPlan = maxPlacesPerPlan;
        this.minScore = minScore;
        this.candidatesPerQuery = candidatesPerQuery;
        this.cacheTtlMillis = Duration.ofHours(Math.max(1, cacheTtlHours)).toMillis();
        this.restClient = RestClient.builder()
                .baseUrl("https://places.googleapis.com")
                .build();
    }

    public AiPlanResponse verifyPlan(AiPlanResponse plan) {
        if (plan == null || plan.itineraries() == null) {
            return plan;
        }
        if (!enabled || apiKey == null || apiKey.isBlank()) {
            log.info("[PLACES-VERIFY] event=skip enabled={} apiKeyPresent={}", enabled, apiKey != null && !apiKey.isBlank());
            return plan;
        }

        int verifiedCount = 0;
        List<AiPlanResponse.DayItinerary> normalizedDays = new ArrayList<>();

        for (AiPlanResponse.DayItinerary day : plan.itineraries()) {
            if (day == null || day.places() == null) {
                normalizedDays.add(day);
                continue;
            }

            List<AiPlanResponse.PlaceDetail> sourcePlaces = day.places();
            List<AiPlanResponse.PlaceDetail> normalizedPlaces = new ArrayList<>();

            for (int i = 0; i < sourcePlaces.size(); i++) {
                AiPlanResponse.PlaceDetail place = sourcePlaces.get(i);
                if (place == null) {
                    continue;
                }

                if (verifiedCount >= Math.max(1, maxPlacesPerPlan)) {
                    normalizedPlaces.add(place);
                    continue;
                }

                String previousPlaceName = i > 0 ? safeName(sourcePlaces.get(i - 1)) : "";
                String nextPlaceName = i < sourcePlaces.size() - 1 ? safeName(sourcePlaces.get(i + 1)) : "";

                normalizedPlaces.add(verifyPlace(place, plan.region(), previousPlaceName, nextPlaceName));
                verifiedCount++;
            }

            normalizedDays.add(new AiPlanResponse.DayItinerary(day.day(), day.theme(), normalizedPlaces));
        }

        log.info("[PLACES-VERIFY] event=complete verifiedCount={} cacheSize={}", verifiedCount, searchCache.size());
        return new AiPlanResponse(
                plan.title(),
                plan.region(),
                plan.accommodation(),
                plan.startDate(),
                plan.endDate(),
                normalizedDays
        );
    }

    public AccommodationVerification verifyAccommodation(String accommodation, String region) {
        if (accommodation == null || accommodation.isBlank()) {
            return null;
        }

        String trimmedAccommodation = accommodation.trim();
        if (!enabled || apiKey == null || apiKey.isBlank()) {
            return new AccommodationVerification(trimmedAccommodation, null, null, null, null, null, true);
        }

        List<QuerySpec> queries = List.of(
                new QuerySpec((safe(region) + " " + trimmedAccommodation).trim(), "ACCOMMODATION_REGION_NAME"),
                new QuerySpec((safe(region) + " " + trimmedAccommodation + " 호텔").trim(), "ACCOMMODATION_HOTEL_HINT"),
                new QuerySpec(trimmedAccommodation, "ACCOMMODATION_NAME_ONLY")
        );

        Map<String, ScoredCandidate> bestByPlaceId = new LinkedHashMap<>();
        AiPlanResponse.PlaceDetail requested = new AiPlanResponse.PlaceDetail(
                trimmedAccommodation,
                "숙소 호텔 체크인",
                null,
                null,
                null
        );

        for (QuerySpec query : queries) {
            if (query.query().isBlank()) {
                continue;
            }
            List<GooglePlaceCandidate> candidates = searchCandidates(region, query.query());
            for (GooglePlaceCandidate candidate : candidates) {
                ScoredCandidate scored = scoreCandidate(requested, region, query.strategy(), candidate);
                String dedupeKey = candidate.googlePlaceId() == null || candidate.googlePlaceId().isBlank()
                        ? normalize(candidate.name()) + "@" + normalize(candidate.address())
                        : candidate.googlePlaceId();
                ScoredCandidate existing = bestByPlaceId.get(dedupeKey);
                if (existing == null || scored.score() > existing.score()) {
                    bestByPlaceId.put(dedupeKey, scored);
                }
            }
        }

        List<ScoredCandidate> ranked = bestByPlaceId.values().stream()
                .sorted(Comparator.comparingInt(ScoredCandidate::score).reversed())
                .toList();

        if (ranked.isEmpty()) {
            log.info("[PLACES-VERIFY] event=accommodation_no_candidate accommodation='{}' region='{}'", trimmedAccommodation, region);
            return new AccommodationVerification(trimmedAccommodation, null, null, null, null, 0.0, true);
        }

        ScoredCandidate best = ranked.get(0);
        boolean uncertain = best.score() < minScore;
        GooglePlaceCandidate candidate = best.candidate();

        log.info(
                "[PLACES-VERIFY] event=accommodation_match accommodation='{}' region='{}' score={} threshold={} accepted={} matchedBy={}",
                trimmedAccommodation,
                region,
                best.score(),
                minScore,
                !uncertain,
                best.matchedBy()
        );

        if (uncertain) {
            return new AccommodationVerification(trimmedAccommodation, null, null, null, null, toConfidence(best.score()), true);
        }

        return new AccommodationVerification(
                candidate.name() == null || candidate.name().isBlank() ? trimmedAccommodation : candidate.name(),
                candidate.googlePlaceId(),
                candidate.lat(),
                candidate.lng(),
                candidate.address(),
                toConfidence(best.score()),
                false
        );
    }

    private AiPlanResponse.PlaceDetail verifyPlace(
            AiPlanResponse.PlaceDetail place,
            String region,
            String previousPlaceName,
            String nextPlaceName
    ) {
        if (place.name() == null || place.name().isBlank()) {
            return place;
        }

        List<QuerySpec> queries = buildQueries(region, place.name(), place.memo(), previousPlaceName, nextPlaceName);
        Map<String, ScoredCandidate> bestByPlaceId = new LinkedHashMap<>();

        for (QuerySpec query : queries) {
            List<GooglePlaceCandidate> candidates = searchCandidates(region, query.query());
            for (GooglePlaceCandidate candidate : candidates) {
                ScoredCandidate scored = scoreCandidate(place, region, query.strategy(), candidate);
                String dedupeKey = candidate.googlePlaceId() == null || candidate.googlePlaceId().isBlank()
                        ? normalize(candidate.name()) + "@" + normalize(candidate.address())
                        : candidate.googlePlaceId();
                ScoredCandidate existing = bestByPlaceId.get(dedupeKey);
                if (existing == null || scored.score() > existing.score()) {
                    bestByPlaceId.put(dedupeKey, scored);
                }
            }
        }

        List<ScoredCandidate> ranked = bestByPlaceId.values().stream()
                .sorted(Comparator.comparingInt(ScoredCandidate::score).reversed())
                .toList();

        if (ranked.isEmpty()) {
            log.info("[PLACES-VERIFY] event=no_candidate place='{}' region='{}'", place.name(), region);
            return place.withVerification(0.0, "UNVERIFIED", true, List.of());
        }

        ScoredCandidate best = ranked.get(0);
        List<AiPlanResponse.PlaceCandidate> top3 = ranked.stream()
                .limit(3)
                .map(this::toDtoCandidate)
                .toList();

        boolean uncertain = best.score() < minScore;
        boolean accepted = !uncertain;

        log.info(
                "[PLACES-VERIFY] event=match place='{}' region='{}' score={} threshold={} accepted={} matchedBy={} topCandidates={}",
                place.name(),
                region,
                best.score(),
                minScore,
                accepted,
                best.matchedBy(),
                top3.stream().map(AiPlanResponse.PlaceCandidate::name).toList()
        );

        if (uncertain) {
            return place.withVerification(toConfidence(best.score()), best.matchedBy(), true, top3);
        }

        return new AiPlanResponse.PlaceDetail(
                best.candidate().name(),
                place.memo(),
                place.startTime(),
                place.durationMinutes(),
                place.category(),
                best.candidate().lat() != null ? best.candidate().lat() : place.lat(),
                best.candidate().lng() != null ? best.candidate().lng() : place.lng(),
                best.candidate().googlePlaceId() != null ? best.candidate().googlePlaceId() : place.googlePlaceId(),
                toConfidence(best.score()),
                best.matchedBy(),
                false,
                top3
        );
    }

    private List<QuerySpec> buildQueries(
            String region,
            String placeName,
            String memo,
            String previousPlaceName,
            String nextPlaceName
    ) {
        Set<QuerySpec> queries = new LinkedHashSet<>();

        String trimmedPlace = placeName == null ? "" : placeName.trim();
        String trimmedRegion = region == null ? "" : region.trim();
        String hint = inferHint(memo);
        String adjacentHint = !previousPlaceName.isBlank() ? previousPlaceName : nextPlaceName;

        if (!trimmedRegion.isBlank()) {
            queries.add(new QuerySpec((trimmedRegion + " " + trimmedPlace).trim(), "REGION_NAME"));
        }
        queries.add(new QuerySpec(trimmedPlace, "NAME_ONLY"));

        if (!adjacentHint.isBlank()) {
            queries.add(new QuerySpec((trimmedPlace + " near " + adjacentHint).trim(), "ADJACENT_PLACE"));
            if (!trimmedRegion.isBlank()) {
                queries.add(new QuerySpec((trimmedRegion + " " + trimmedPlace + " near " + adjacentHint).trim(), "ADJACENT_PLACE_REGION"));
            }
        }

        if (!hint.isBlank()) {
            queries.add(new QuerySpec((trimmedPlace + " " + hint).trim(), "TYPE_HINT"));
            if (!trimmedRegion.isBlank()) {
                queries.add(new QuerySpec((trimmedRegion + " " + trimmedPlace + " " + hint).trim(), "TYPE_HINT_REGION"));
            }
        }

        return new ArrayList<>(queries);
    }

    private List<GooglePlaceCandidate> searchCandidates(String region, String textQuery) {
        if (textQuery == null || textQuery.isBlank()) {
            return List.of();
        }

        String cacheKey = (normalize(region) + "|" + normalize(textQuery) + "|" + languageCode).trim();
        long now = System.currentTimeMillis();

        CacheEntry cacheEntry = searchCache.get(cacheKey);
        if (cacheEntry != null && cacheEntry.expiresAt() > now) {
            return cacheEntry.candidates();
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("textQuery", textQuery);
        body.put("languageCode", languageCode);
        body.put("maxResultCount", Math.max(1, candidatesPerQuery));

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
                return List.of();
            }

            JsonNode root = objectMapper.readTree(response);
            JsonNode places = root.path("places");
            if (!places.isArray() || places.isEmpty()) {
                return List.of();
            }

            List<GooglePlaceCandidate> candidates = new ArrayList<>();
            for (JsonNode node : places) {
                candidates.add(toCandidate(node));
            }

            searchCache.put(cacheKey, new CacheEntry(now + cacheTtlMillis, List.copyOf(candidates)));
            if (searchCache.size() > 2000) {
                evictExpired(now);
            }
            return candidates;
        } catch (Exception e) {
            log.warn("[PLACES-VERIFY] event=search_error query='{}' region='{}' message={}", textQuery, region, e.getMessage());
            return List.of();
        }
    }

    private void evictExpired(long now) {
        searchCache.entrySet().removeIf(entry -> entry.getValue().expiresAt() <= now);
    }

    private ScoredCandidate scoreCandidate(
            AiPlanResponse.PlaceDetail requested,
            String region,
            String queryStrategy,
            GooglePlaceCandidate candidate
    ) {
        String requestedName = normalize(requested.name());
        String candidateName = normalize(candidate.name());
        String address = normalize(candidate.address());

        int nameScore = scoreName(requestedName, candidateName);
        int regionScore = scoreRegion(normalize(region), candidateName + " " + address);
        int typeScore = scoreTypeHint(requested.memo(), candidate.primaryType(), candidate.types());
        int reputationScore = scoreReputation(candidate.rating(), candidate.userRatingCount());
        int strategyBonus = queryStrategy.startsWith("REGION") ? 3 : 0;

        int total = Math.min(100, nameScore + regionScore + typeScore + reputationScore + strategyBonus);
        return new ScoredCandidate(candidate, total, queryStrategy);
    }

    private int scoreName(String requestedName, String candidateName) {
        if (requestedName.isBlank() || candidateName.isBlank()) {
            return 0;
        }
        if (requestedName.equals(candidateName)) {
            return 70;
        }
        if (candidateName.contains(requestedName) || requestedName.contains(candidateName)) {
            return 60;
        }

        Set<String> left = tokenSet(requestedName);
        Set<String> right = tokenSet(candidateName);
        if (left.isEmpty() || right.isEmpty()) {
            return 0;
        }

        int intersection = 0;
        for (String token : left) {
            if (right.contains(token)) {
                intersection++;
            }
        }

        int union = left.size() + right.size() - intersection;
        double similarity = union == 0 ? 0 : (double) intersection / union;
        return (int) Math.round(50 * similarity);
    }

    private int scoreRegion(String normalizedRegion, String candidateText) {
        if (normalizedRegion == null || normalizedRegion.isBlank()) {
            return 0;
        }
        int score = 0;
        if (candidateText.contains(normalizedRegion)) {
            score += 8;
        }

        for (String token : tokenSet(normalizedRegion)) {
            if (token.length() >= 2 && candidateText.contains(token)) {
                score += 3;
            }
        }
        return Math.min(15, score);
    }

    private int scoreTypeHint(String memo, String primaryType, List<String> types) {
        String hint = inferHint(memo);
        if (hint.isBlank()) {
            return 0;
        }

        String typeText = normalize((primaryType == null ? "" : primaryType) + " " + String.join(" ", types == null ? List.of() : types));

        if (hint.equals("restaurant") && (typeText.contains("restaurant") || typeText.contains("cafe") || typeText.contains("food"))) {
            return 10;
        }
        if (hint.equals("lodging") && (typeText.contains("lodging") || typeText.contains("hotel"))) {
            return 10;
        }
        if (hint.equals("tourist_attraction") && (typeText.contains("tourist_attraction") || typeText.contains("museum") || typeText.contains("park"))) {
            return 10;
        }
        if (hint.equals("shopping") && (typeText.contains("market") || typeText.contains("shopping") || typeText.contains("store"))) {
            return 8;
        }
        return 0;
    }

    private int scoreReputation(Double rating, Integer userRatingCount) {
        int score = 0;
        if (rating != null) {
            score += (int) Math.round(Math.max(0.0, Math.min(5.0, rating)) / 5.0 * 3.0);
        }
        if (userRatingCount != null && userRatingCount > 0) {
            score += userRatingCount >= 100 ? 2 : 1;
        }
        return Math.min(5, score);
    }

    private String inferHint(String memo) {
        String normalized = normalize(memo);
        if (normalized.isBlank()) {
            return "";
        }

        if (containsAny(normalized, "맛집", "식당", "음식", "저녁", "점심", "카페", "브런치")) {
            return "restaurant";
        }
        if (containsAny(normalized, "호텔", "숙소", "체크인", "리조트", "펜션")) {
            return "lodging";
        }
        if (containsAny(normalized, "시장", "쇼핑", "기념품", "백화점", "몰")) {
            return "shopping";
        }
        if (containsAny(normalized, "박물관", "미술관", "공원", "해변", "관광", "명소", "유적", "사찰")) {
            return "tourist_attraction";
        }
        return "";
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        String cleaned = NON_WORD.matcher(value.toLowerCase()).replaceAll(" ");
        return SPACES.matcher(cleaned).replaceAll(" ").trim();
    }

    private Set<String> tokenSet(String value) {
        Set<String> tokens = new LinkedHashSet<>();
        if (value == null || value.isBlank()) {
            return tokens;
        }
        for (String token : value.split(" ")) {
            String t = token.trim();
            if (!t.isBlank()) {
                tokens.add(t);
            }
        }
        return tokens;
    }

    private String safeName(AiPlanResponse.PlaceDetail place) {
        if (place == null || place.name() == null) {
            return "";
        }
        return place.name().trim();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private GooglePlaceCandidate toCandidate(JsonNode node) {
        String name = text(node.path("displayName"), "text", "");
        String placeId = text(node, "id", null);
        Double lat = number(node.path("location"), "latitude", null);
        Double lng = number(node.path("location"), "longitude", null);
        String address = text(node, "formattedAddress", "");
        String primaryType = text(node, "primaryType", "");
        Double rating = number(node, "rating", null);
        Integer userRatingCount = integer(node, "userRatingCount", null);

        List<String> types = new ArrayList<>();
        JsonNode typesNode = node.path("types");
        if (typesNode.isArray()) {
            for (JsonNode t : typesNode) {
                if (t.isTextual()) {
                    types.add(t.asText());
                }
            }
        }

        return new GooglePlaceCandidate(
                name,
                placeId,
                lat,
                lng,
                address,
                primaryType,
                types,
                rating,
                userRatingCount
        );
    }

    private AiPlanResponse.PlaceCandidate toDtoCandidate(ScoredCandidate scored) {
        GooglePlaceCandidate c = scored.candidate();
        return new AiPlanResponse.PlaceCandidate(
                c.name(),
                c.googlePlaceId(),
                c.lat(),
                c.lng(),
                c.address(),
                c.primaryType(),
                c.rating(),
                c.userRatingCount(),
                toConfidence(scored.score()),
                scored.matchedBy()
        );
    }

    private Double toConfidence(int score) {
        return Math.round((score / 100.0) * 100.0) / 100.0;
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

    private Integer integer(JsonNode node, String field, Integer fallback) {
        JsonNode value = node.path(field);
        if (value.isInt() || value.isLong()) {
            return value.asInt();
        }
        return fallback;
    }

    private record QuerySpec(String query, String strategy) {
    }

    private record GooglePlaceCandidate(
            String name,
            String googlePlaceId,
            Double lat,
            Double lng,
            String address,
            String primaryType,
            List<String> types,
            Double rating,
            Integer userRatingCount
    ) {
    }

    private record ScoredCandidate(GooglePlaceCandidate candidate, int score, String matchedBy) {
    }

    private record CacheEntry(long expiresAt, List<GooglePlaceCandidate> candidates) {
    }

    public record AccommodationVerification(
            String name,
            String googlePlaceId,
            Double lat,
            Double lng,
            String address,
            Double confidence,
            boolean verificationUncertain
    ) {
    }
}

