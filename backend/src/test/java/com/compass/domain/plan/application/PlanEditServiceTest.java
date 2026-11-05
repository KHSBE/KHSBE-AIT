package com.compass.domain.plan.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.compass.domain.agent.application.PlaceCatalog;
import com.compass.domain.plan.dao.*;
import com.compass.domain.plan.domain.*;
import com.compass.domain.plan.dto.*;
import com.compass.domain.user.dao.UserRepository;
import com.compass.domain.user.domain.User;
import com.compass.global.config.QuerydslConfig;
import com.compass.global.error.BusinessException;
import com.fasterxml.jackson.databind.node.MissingNode;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.*;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.TransactionTemplate;

@DataJpaTest(
    properties = {
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
      "spring.jpa.show-sql=false"
    })
@Import({PlanEditService.class, QuerydslConfig.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PlanEditServiceTest {
  @Autowired PlanEditService service;
  @Autowired TravelPlanRepository plans;
  @Autowired UserRepository users;
  @Autowired TransactionTemplate transactions;
  @MockitoSpyBean PlaceRepository places;
  @MockitoBean PlaceCatalog catalog;
  User owner;
  Long planId;
  List<Long> dayIds;

  @BeforeEach
  void seed() {
    owner = users.save(User.builder().email(UUID.randomUUID() + "@test.com").build());
    transactions.execute(
        status -> {
          var plan =
              TravelPlan.builder()
                  .user(owner)
                  .title("trip")
                  .destinationPlaceId("city-123")
                  .startDate(LocalDate.of(2026, 12, 1))
                  .endDate(LocalDate.of(2026, 12, 2))
                  .build();
          for (int n = 1; n <= 2; n++) {
            var day =
                Itinerary.builder()
                    .travelPlan(plan)
                    .dayNumber(n)
                    .travelDate(plan.getStartDate().plusDays(n - 1))
                    .build();
            day.getPlaces()
                .add(
                    Place.builder()
                        .itinerary(day)
                        .placeName("original-" + n)
                        .googlePlaceId("old-place-" + n)
                        .visitOrder(1)
                        .build());
            plan.getItineraries().add(day);
          }
          plans.saveAndFlush(plan);
          planId = plan.getId();
          dayIds = plan.getItineraries().stream().map(Itinerary::getId).toList();
          return null;
        });
  }

  PlaceCatalog.PlaceInfo info(String id) {
    return new PlaceCatalog.PlaceInfo(
        id,
        "server-" + id,
        "address",
        1,
        1,
        "KR",
        List.of("tourist_attraction"),
        "OPERATIONAL",
        0,
        0,
        10,
        10,
        MissingNode.getInstance(),
        MissingNode.getInstance(),
        "https://maps.google.com/?cid=123");
  }

  UpdatePlanRequest request() {
    return new UpdatePlanRequest(
        List.of(
            new UpdatePlanRequest.Day(dayIds.get(0), List.of(item("new-place-1"))),
            new UpdatePlanRequest.Day(dayIds.get(1), List.of(item("new-place-2")))));
  }

  UpdateItineraryPlacesRequest.PlaceItem item(String id) {
    return new UpdateItineraryPlacesRequest.PlaceItem(
        "untrusted", id, "10:00", 60, "관광", -90.0, -180.0, "memo", 100.0, "FAKE", false);
  }

  void allowPlaces() {
    when(catalog.details("city-123")).thenReturn(info("city-123"));
    when(catalog.details("new-place-1")).thenReturn(info("new-place-1"));
    when(catalog.details("new-place-2")).thenReturn(info("new-place-2"));
  }

  List<String> names() {
    return transactions.execute(
        status ->
            plans.findById(planId).orElseThrow().getItineraries().stream()
                .sorted(Comparator.comparing(Itinerary::getDayNumber))
                .map(d -> d.getPlaces().get(0).getPlaceName())
                .toList());
  }

  @Test
  void secondDayFailureRollsBackEveryDay() {
    allowPlaces();
    doThrow(new IllegalStateException("simulated write failure"))
        .when(places)
        .deleteByItineraryId(dayIds.get(1));
    assertThrows(IllegalStateException.class, () -> service.update(owner, planId, request()));
    assertEquals(List.of("original-1", "original-2"), names());
  }

  @Test
  void failedFinalPlaceDoesNotStartWrites() {
    allowPlaces();
    when(catalog.details("new-place-2"))
        .thenThrow(
            new BusinessException("unavailable", org.springframework.http.HttpStatus.BAD_GATEWAY));
    assertThrows(BusinessException.class, () -> service.update(owner, planId, request()));
    verify(places, never()).deleteByItineraryId(anyLong());
    assertEquals(List.of("original-1", "original-2"), names());
  }

  @Test
  void successfulBatchTrustsServerFieldsNotClientVerification() {
    allowPlaces();
    service.update(owner, planId, request());
    assertEquals(List.of("server-new-place-1", "server-new-place-2"), names());
    transactions.execute(
        status -> {
          var place =
              plans.findById(planId).orElseThrow().getItineraries().get(0).getPlaces().get(0);
          assertEquals(1.0, place.getLatitude());
          assertNull(place.getConfidence());
          assertEquals("USER_SELECTED_PLACES", place.getMatchedBy());
          return null;
        });
  }

  @Test
  void anotherUserCannotReadOrChangePlan() {
    var stranger = users.save(User.builder().email(UUID.randomUUID() + "@test.com").build());
    assertThrows(BusinessException.class, () -> service.update(stranger, planId, request()));
    verifyNoInteractions(catalog);
  }
}
