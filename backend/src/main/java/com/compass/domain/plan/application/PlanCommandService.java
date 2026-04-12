package com.compass.domain.plan.application;

import com.compass.domain.agent.dto.AiPlanResponse;
import com.compass.domain.plan.dao.PlaceRepository;
import com.compass.domain.plan.dao.TravelPlanRepository;
import com.compass.domain.plan.domain.Itinerary;
import com.compass.domain.plan.domain.Place;
import com.compass.domain.plan.domain.TravelPlan;
import com.compass.domain.plan.dto.UpdateItineraryPlacesRequest;
import com.compass.domain.user.domain.User;
import com.compass.global.error.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PlanCommandService {

    private final TravelPlanRepository planRepository;
    private final PlaceRepository placeRepository;

    public Long savePlanFromAi(User user, AiPlanResponse res) {
        try {
            if (res == null) {
                throw new IllegalArgumentException("AI response is null");
            }
            if (res.startDate() == null || res.endDate() == null) {
                throw new IllegalArgumentException("AI response does not contain valid dates");
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
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[PLAN-SAVE] failed to save AI plan. userId={}, title={}, message={}",
                    user != null ? user.getId() : null,
                    res != null ? res.title() : null,
                    e.getMessage(),
                    e);
            throw new BusinessException("AI 일정 저장에 실패했습니다. 잠시 후 다시 시도해 주세요.", HttpStatus.BAD_GATEWAY);
        }
    }

    public TravelPlan updateItineraryPlaces(User user, Long planId, Long itineraryId, UpdateItineraryPlacesRequest request) {
        TravelPlan plan = planRepository.findByIdAndUserIdWithDetails(planId, user.getId())
                .orElseThrow(() -> new BusinessException("Plan not found.", HttpStatus.NOT_FOUND));

        Itinerary itinerary = plan.getItineraries().stream()
                .filter(item -> Objects.equals(item.getId(), itineraryId))
                .findFirst()
                .orElseThrow(() -> new BusinessException("Itinerary not found in this plan.", HttpStatus.NOT_FOUND));

        placeRepository.deleteByItineraryId(itineraryId);
        itinerary.getPlaces().clear();

        int visitOrder = 1;
        for (UpdateItineraryPlacesRequest.PlaceItem item : request.places()) {
            itinerary.getPlaces().add(
                    Place.builder()
                            .itinerary(itinerary)
                            .placeName(item.placeName().trim())
                            .googlePlaceId(item.googlePlaceId())
                            .latitude(item.latitude() != null ? item.latitude() : 0.0)
                            .longitude(item.longitude() != null ? item.longitude() : 0.0)
                            .visitOrder(visitOrder++)
                            .memo(item.memo())
                            .confidence(item.confidence())
                            .matchedBy(item.matchedBy())
                            .verificationUncertain(item.verificationUncertain() != null ? item.verificationUncertain() : false)
                            .build()
            );
        }

        return plan;
    }

    private void mapItineraries(TravelPlan plan, AiPlanResponse res) {
        if (res.itineraries() == null) {
            return;
        }

        for (var day : res.itineraries()) {
            var itinerary = Itinerary.builder()
                    .travelPlan(plan)
                    .dayNumber(day.day())
                    .travelDate(plan.getStartDate().plusDays(day.day() - 1))
                    .theme(day.theme())
                    .build();

            if (day.places() != null) {
                int visitOrder = 1;
                for (var p : day.places()) {
                    itinerary.getPlaces().add(Place.builder()
                            .itinerary(itinerary)
                            .placeName(p.name())
                            .googlePlaceId(p.googlePlaceId())
                            .latitude(p.lat() != null ? p.lat() : 0.0)
                            .longitude(p.lng() != null ? p.lng() : 0.0)
                            .visitOrder(visitOrder++)
                            .memo(p.memo())
                            .confidence(p.confidence())
                            .matchedBy(p.matchedBy())
                            .verificationUncertain(p.verificationUncertain())
                            .build());
                }
            }
            plan.getItineraries().add(itinerary);
        }
    }
}
