package com.compass.domain.agent.dto;

import java.util.List;

// AI 일정 생성 응답 규격
public record AiPlanResponse(
        String title,
        String region,
        String accommodation,
        String startDate,
        String endDate,
        List<DayItinerary> itineraries
) {
    public record DayItinerary(
            int day,
            String theme,
            List<PlaceDetail> places
    ) {}

    public record PlaceDetail(
            String name,
            String memo,
            Double lat,
            Double lng,
            String googlePlaceId
    ) {}
}
