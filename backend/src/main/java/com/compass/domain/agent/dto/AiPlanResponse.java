package com.compass.domain.agent.dto;

import java.util.List;

public record AiPlanResponse(
        Long planId,
        String title,
        String region,
        String accommodation,
        String startDate,
        String endDate,
        List<DayItinerary> itineraries
) {
    public AiPlanResponse(
            String title,
            String region,
            String accommodation,
            String startDate,
            String endDate,
            List<DayItinerary> itineraries
    ) {
        this(null, title, region, accommodation, startDate, endDate, itineraries);
    }

    public record DayItinerary(
            Long itineraryId,
            int day,
            String theme,
            List<PlaceDetail> places
    ) {
        public DayItinerary(int day, String theme, List<PlaceDetail> places) {
            this(null, day, theme, places);
        }
    }

    public record PlaceDetail(
            String name,
            String memo,
            Double lat,
            Double lng,
            String googlePlaceId,
            Double confidence,
            String matchedBy,
            boolean verificationUncertain,
            List<PlaceCandidate> candidates
    ) {
        public PlaceDetail(String name, String memo, Double lat, Double lng, String googlePlaceId) {
            this(name, memo, lat, lng, googlePlaceId, null, null, false, List.of());
        }

        public PlaceDetail withVerification(Double confidence, String matchedBy, boolean verificationUncertain, List<PlaceCandidate> candidates) {
            return new PlaceDetail(
                    name,
                    memo,
                    lat,
                    lng,
                    googlePlaceId,
                    confidence,
                    matchedBy,
                    verificationUncertain,
                    candidates == null ? List.of() : List.copyOf(candidates)
            );
        }
    }

    public record PlaceCandidate(
            String name,
            String googlePlaceId,
            Double lat,
            Double lng,
            String address,
            String primaryType,
            Double rating,
            Integer userRatingCount,
            Double confidence,
            String matchedBy
    ) {
    }
}
