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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanCommandServiceTest {

    @Mock
    private TravelPlanRepository planRepository;

    @Mock
    private PlaceRepository placeRepository;

    private PlanCommandService planCommandService;

    @BeforeEach
    void setUp() {
        planCommandService = new PlanCommandService(planRepository, placeRepository);
    }

    @Test
    void updateItineraryPlaces_shouldDeny_whenPlanIsNotOwnedByUser() {
        User user = User.builder().id(10L).email("owner@test.com").build();
        when(planRepository.findByIdAndUserIdWithDetails(100L, 10L)).thenReturn(Optional.empty());

        UpdateItineraryPlacesRequest request = new UpdateItineraryPlacesRequest(List.of(
                new UpdateItineraryPlacesRequest.PlaceItem("A", null, 0.0, 0.0, "memo")
        ));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> planCommandService.updateItineraryPlaces(user, 100L, 1L, request)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
    }

    @Test
    void savePlanFromAi_shouldPersistMappedPlanAndPlaces() {
        User user = User.builder().id(1L).email("u1@test.com").build();
        AiPlanResponse response = new AiPlanResponse(
                "부산 여행",
                "부산",
                "해운대 호텔",
                "2026-04-10",
                "2026-04-11",
                List.of(
                        new AiPlanResponse.DayItinerary(
                                1,
                                "해운대 코스",
                                List.of(
                                        new AiPlanResponse.PlaceDetail(
                                                "해운대 해수욕장",
                                                "아침 산책",
                                                35.1587,
                                                129.1604,
                                                "pid-1"
                                        )
                                )
                        )
                )
        );

        when(planRepository.save(any(TravelPlan.class))).thenAnswer(invocation -> {
            TravelPlan plan = invocation.getArgument(0, TravelPlan.class);
            plan.setId(777L);
            return plan;
        });

        Long savedId = planCommandService.savePlanFromAi(user, response);
        assertEquals(777L, savedId);

        ArgumentCaptor<TravelPlan> captor = ArgumentCaptor.forClass(TravelPlan.class);
        verify(planRepository).save(captor.capture());
        TravelPlan saved = captor.getValue();

        assertEquals("부산 여행", saved.getTitle());
        assertEquals(LocalDate.of(2026, 4, 10), saved.getStartDate());
        assertEquals(1, saved.getItineraries().size());

        Itinerary itinerary = saved.getItineraries().get(0);
        assertEquals(1, itinerary.getDayNumber());
        assertEquals(1, itinerary.getPlaces().size());

        Place place = itinerary.getPlaces().get(0);
        assertEquals("해운대 해수욕장", place.getPlaceName());
        assertEquals("pid-1", place.getGooglePlaceId());
        assertNotNull(place.getVisitOrder());
        assertEquals(1, place.getVisitOrder());
    }

    @Test
    void updateItineraryPlaces_shouldReplacePlacesInOrder() {
        User user = User.builder().id(1L).email("u1@test.com").build();

        Itinerary itinerary = Itinerary.builder()
                .id(200L)
                .dayNumber(1)
                .travelDate(LocalDate.of(2026, 4, 10))
                .theme("old")
                .build();
        itinerary.getPlaces().add(Place.builder()
                .itinerary(itinerary)
                .placeName("old-place")
                .visitOrder(1)
                .latitude(0.0)
                .longitude(0.0)
                .build());

        TravelPlan plan = TravelPlan.builder()
                .id(100L)
                .user(user)
                .title("t")
                .region("r")
                .accommodation("a")
                .startDate(LocalDate.of(2026, 4, 10))
                .endDate(LocalDate.of(2026, 4, 12))
                .itineraries(new ArrayList<>(List.of(itinerary)))
                .build();

        when(planRepository.findByIdAndUserIdWithDetails(100L, 1L)).thenReturn(Optional.of(plan));

        UpdateItineraryPlacesRequest request = new UpdateItineraryPlacesRequest(List.of(
                new UpdateItineraryPlacesRequest.PlaceItem("A", "pid-A", 35.1, 129.1, "m1"),
                new UpdateItineraryPlacesRequest.PlaceItem("B", "pid-B", 35.2, 129.2, "m2")
        ));

        TravelPlan updated = planCommandService.updateItineraryPlaces(user, 100L, 200L, request);
        verify(placeRepository).deleteByItineraryId(200L);

        Itinerary updatedItinerary = updated.getItineraries().get(0);
        assertEquals(2, updatedItinerary.getPlaces().size());
        assertEquals("A", updatedItinerary.getPlaces().get(0).getPlaceName());
        assertEquals(1, updatedItinerary.getPlaces().get(0).getVisitOrder());
        assertEquals("B", updatedItinerary.getPlaces().get(1).getPlaceName());
        assertEquals(2, updatedItinerary.getPlaces().get(1).getVisitOrder());
    }
}
