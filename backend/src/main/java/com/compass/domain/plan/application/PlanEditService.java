package com.compass.domain.plan.application;

import com.compass.domain.agent.application.PlaceCatalog;
import com.compass.domain.plan.dao.*;
import com.compass.domain.plan.domain.*;
import com.compass.domain.plan.dto.UpdatePlanRequest;
import com.compass.domain.user.domain.User;
import com.compass.global.error.BusinessException;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class PlanEditService {
  private final TravelPlanRepository plans;
  private final PlaceRepository places;
  private final PlaceCatalog catalog;
  private final TransactionTemplate transactions;
  private final com.compass.domain.user.dao.UserRepository users;

  public TravelPlan update(User user, Long id, UpdatePlanRequest request) {
    var snapshot =
        transactions.execute(
            status -> {
              var plan =
                  plans.findByIdAndUserIdWithDetails(id, user.getId()).orElseThrow(this::missing);
              return new Snapshot(
                  plan.getDestinationPlaceId(),
                  plan.getItineraries().stream()
                      .map(Itinerary::getId)
                      .collect(java.util.stream.Collectors.toSet()));
            });
    if (snapshot.destinationId() == null)
      throw new BusinessException("이전 버전 일정입니다. 새 여행 조건으로 다시 생성해 주세요.", HttpStatus.CONFLICT);
    var destination = catalog.details(snapshot.destinationId());
    var expected = snapshot.dayIds();
    var supplied =
        request.days().stream()
            .map(UpdatePlanRequest.Day::itineraryId)
            .collect(java.util.stream.Collectors.toSet());
    if (!expected.equals(supplied) || supplied.size() != request.days().size())
      throw new BusinessException("모든 날짜를 중복 없이 전달해 주세요.", HttpStatus.BAD_REQUEST);
    Map<String, PlaceCatalog.PlaceInfo> checked = new HashMap<>();
    Set<String> used = new HashSet<>();
    for (var day : request.days()) {
      int previous = -20;
      for (var item : day.places()) {
        String pid = PlaceCatalog.normalizeId(item.googlePlaceId());
        if (!used.add(pid)) throw new BusinessException("같은 장소가 중복됐습니다.", HttpStatus.BAD_REQUEST);
        var place = catalog.details(pid);
        if (!destination.contains(place) || !place.available())
          throw new BusinessException("여행 지역 안의 영업 가능한 장소를 선택해 주세요.", HttpStatus.BAD_REQUEST);
        checked.put(pid, place);
        if (item.startTime() != null && !item.startTime().isBlank()) {
          int time;
          try {
            time = java.time.LocalTime.parse(item.startTime()).toSecondOfDay() / 60;
          } catch (Exception e) {
            throw new BusinessException("방문 시각을 확인해 주세요.", HttpStatus.BAD_REQUEST);
          }
          if (item.durationMinutes() == null
              || item.durationMinutes() < 1
              || item.durationMinutes() > 360
              || time < previous + 20
              || time + item.durationMinutes() > 1440)
            throw new BusinessException(
                "방문 시각과 체류시간이 겹칩니다. 시간도 함께 조정해 주세요.", HttpStatus.BAD_REQUEST);
          previous = time + item.durationMinutes();
        }
      }
    }
    return transactions.execute(
        status -> {
          users.lockById(user.getId()).orElseThrow(this::missing);
          var plan =
              plans.findByIdAndUserIdWithDetails(id, user.getId()).orElseThrow(this::missing);
          // A single transaction covers every day; API-supplied verification flags are ignored.
          for (var input : request.days()) {
            var day =
                plan.getItineraries().stream()
                    .filter(d -> d.getId().equals(input.itineraryId()))
                    .findFirst()
                    .orElseThrow(this::missing);
            places.deleteByItineraryId(day.getId());
            day.getPlaces().clear();
            int order = 1;
            for (var item : input.places()) {
              var p = checked.get(PlaceCatalog.normalizeId(item.googlePlaceId()));
              day.getPlaces()
                  .add(
                      Place.builder()
                          .itinerary(day)
                          .placeName(p.name())
                          .googlePlaceId(p.id())
                          .latitude(p.lat())
                          .longitude(p.lng())
                          .startTime(item.startTime())
                          .durationMinutes(item.durationMinutes())
                          .category(item.category())
                          .memo(item.memo())
                          .visitOrder(order++)
                          .matchedBy("USER_SELECTED_PLACES")
                          .confidence(null)
                          .verificationUncertain(false)
                          .sourceUri(p.mapsUri())
                          .build());
            }
          }
          return plan;
        });
  }

  private record Snapshot(String destinationId, Set<Long> dayIds) {}

  private BusinessException missing() {
    return new BusinessException("일정을 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
  }
}
