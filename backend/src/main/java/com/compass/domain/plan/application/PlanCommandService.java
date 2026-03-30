package com.compass.domain.plan.application;

import com.compass.domain.plan.dao.TravelPlanRepository;
import com.compass.domain.plan.domain.*;
import com.compass.domain.agent.dto.AiPlanResponse;
import com.compass.domain.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;

// 여행 계획 저장 서비스
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PlanCommandService {

    private final TravelPlanRepository planRepository;

    // ★ 파라미터를 String에서 AiPlanResponse 객체로 바로 받습니다! (ObjectMapper 삭제)
    public Long savePlanFromAi(User user, AiPlanResponse res) {
        try {
            // 필수 필드 체크 (NullPointerException 방지)
            if (res.startDate() == null || res.endDate() == null) {
                throw new IllegalArgumentException("AI 응답에 날짜 정보가 누락되었습니다.");
            }

            var plan = TravelPlan.builder()
                    .user(user)
                    .title(res.title())
                    .region(res.region())
                    .accommodation(res.accommodation())
                    .startDate(LocalDate.parse(res.startDate()))
                    .endDate(LocalDate.parse(res.endDate()))
                    .build();

            mapItineraries(plan, res);
            var savedPlan = planRepository.save(plan);
            return savedPlan.getId();
        } catch (Exception e) {
            log.error("일정 저장 에러 발생. 객체 데이터: {}", res, e);
            throw new RuntimeException("일정 저장 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    // ★ 회원님이 기존에 완벽하게 짜두신 매핑 로직 그대로 사용!
    private void mapItineraries(TravelPlan plan, AiPlanResponse res) {
        if (res.itineraries() == null) return;

        for (var day : res.itineraries()) {
            var itinerary = Itinerary.builder()
                    .travelPlan(plan)
                    .dayNumber(day.day())
                    .travelDate(plan.getStartDate().plusDays(day.day() - 1))
                    .theme(day.theme())
                    .build();

            if (day.places() != null) {
                for (var p : day.places()) {
                    itinerary.getPlaces().add(Place.builder()
                            .itinerary(itinerary)
                            .placeName(p.name())
                            .latitude(p.lat() != null ? p.lat() : 0.0)
                            .longitude(p.lng() != null ? p.lng() : 0.0)
                            .memo(p.memo())
                            .build());
                }
            }
            plan.getItineraries().add(itinerary);
        }
    }
}