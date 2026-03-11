package com.compass.domain.plan.application;

import com.compass.domain.plan.dao.TravelPlanRepository;
import com.compass.domain.plan.domain.Itinerary;
import com.compass.domain.plan.domain.Place;
import com.compass.domain.plan.domain.TravelPlan;
import com.compass.domain.agent.dto.AiPlanResponse;
import com.compass.domain.user.domain.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional

public class PlanCommandService {

    private final TravelPlanRepository travelPlanRepository;
    private final ObjectMapper objectMapper; // JSON 파싱용

    public Long savePlanFromAi(User user, String aiJsonResponse) {
        try {
            // 1. AI 응답 문자열을 DTO로 변환
            AiPlanResponse response = objectMapper.readValue(aiJsonResponse, AiPlanResponse.class);

            // 2. TravelPlan 엔티티 생성
            TravelPlan plan = TravelPlan.builder()
                    .user(user)
                    .title(response.getTitle())
                    .region(response.getRegion())
                    .startDate(LocalDate.parse(response.getStartDate()))
                    .endDate(LocalDate.parse(response.getEndDate()))
                    .build();

            // 3. 일차별 일정(Itinerary) 및 장소(Place) 매핑
            response.getItineraries().forEach(dayDto -> {
                Itinerary itinerary = Itinerary.builder()
                        .travelPlan(plan)
                        .dayNumber(dayDto.getDay())
                        .travelDate(plan.getStartDate().plusDays(dayDto.getDay() - 1))
                        .theme(dayDto.getTheme())
                        .build();

                dayDto.getPlaces().forEach(placeDto -> {
                    Place.builder()
                            .itinerary(itinerary)
                            .placeName(placeDto.getName())
                            .latitude(placeDto.getLat())
                            .longitude(placeDto.getLng())
                            .memo(placeDto.getMemo())
                            .build();
                    // Cascade 설정 덕분에 부모 엔티티 저장 시 함께 저장됨
                });
                plan.getItineraries().add(itinerary);
            });

            return travelPlanRepository.save(plan).getId();
        } catch (Exception e) {
            throw new RuntimeException("AI 일정 저장 중 오류 발생: " + e.getMessage());
        }
    }
}