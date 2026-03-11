package com.compass.domain.agent.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

@Getter
@NoArgsConstructor
public class AiPlanResponse {
    private String title;
    private String region;
    private String startDate;
    private String endDate;
    private List<DayItinerary> itineraries;

    @Getter
    @NoArgsConstructor
    public static class DayItinerary {
        private int day;
        private String theme;
        private List<PlaceDetail> places;
    }

    @Getter
    @NoArgsConstructor
    public static class PlaceDetail {
        private String name;
        private String memo;
        private Double lat;
        private Double lng;
    }
}
