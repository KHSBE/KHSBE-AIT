package com.compass.domain.agent.application;

import com.compass.domain.agent.dto.AiPlanResponse;
import com.compass.global.error.BusinessException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PlanQualityValidator {

  private static final int MIN_PLACES_PER_DAY = 2;
  private static final int MAX_PLACES_PER_DAY = 6;

  public void validate(AiPlanResponse plan) {
    if (plan == null) {
      fail("NULL_PLAN", "AI 일정 응답이 비어 있습니다.");
    }

    requireText(plan.title(), "EMPTY_TITLE", "일정 제목이 비어 있습니다.");
    requireText(plan.region(), "EMPTY_REGION", "여행 지역이 비어 있습니다.");
    requireText(plan.startDate(), "EMPTY_START_DATE", "시작일이 비어 있습니다.");
    requireText(plan.endDate(), "EMPTY_END_DATE", "종료일이 비어 있습니다.");

    List<AiPlanResponse.DayItinerary> days = plan.itineraries();
    if (days == null || days.isEmpty()) {
      fail("EMPTY_ITINERARIES", "일차별 일정이 비어 있습니다.");
    }

    int expectedDays = calculateExpectedDays(plan.startDate(), plan.endDate());
    if (days.size() != expectedDays) {
      fail(
          "DAY_COUNT_MISMATCH",
          "날짜 수와 일차별 일정 수가 맞지 않습니다. expectedDays=%d actualDays=%d",
          expectedDays,
          days.size());
    }

    int totalPlaces = 0;
    Set<Integer> seenDayNumbers = new HashSet<>();

    for (int index = 0; index < days.size(); index++) {
      AiPlanResponse.DayItinerary day = days.get(index);
      if (day == null) {
        fail("NULL_DAY", "비어 있는 일차 데이터가 포함되어 있습니다. index=%d", index);
      }

      int expectedDayNumber = index + 1;
      if (day.day() != expectedDayNumber) {
        fail(
            "INVALID_DAY_ORDER",
            "Day 번호가 순서대로 생성되지 않았습니다. expected=%d actual=%d",
            expectedDayNumber,
            day.day());
      }
      if (!seenDayNumbers.add(day.day())) {
        fail("DUPLICATED_DAY", "중복된 Day 번호가 있습니다. day=%d", day.day());
      }

      requireText(day.theme(), "EMPTY_DAY_THEME", "Day 테마가 비어 있습니다. day=%d", day.day());
      validatePlaces(day);
      totalPlaces += day.places().size();
    }

    int maxTotalPlaces = expectedDays * MAX_PLACES_PER_DAY;
    if (totalPlaces > maxTotalPlaces) {
      fail(
          "TOO_MANY_TOTAL_PLACES",
          "전체 장소 수가 너무 많습니다. max=%d actual=%d",
          maxTotalPlaces,
          totalPlaces);
    }
  }

  private void validatePlaces(AiPlanResponse.DayItinerary day) {
    List<AiPlanResponse.PlaceDetail> places = day.places();
    if (places == null || places.isEmpty()) {
      fail("EMPTY_DAY_PLACES", "Day 장소 목록이 비어 있습니다. day=%d", day.day());
    }

    if (places.size() < MIN_PLACES_PER_DAY || places.size() > MAX_PLACES_PER_DAY) {
      fail(
          "INVALID_PLACES_PER_DAY",
          "하루 장소 수가 기준을 벗어났습니다. day=%d min=%d max=%d actual=%d",
          day.day(),
          MIN_PLACES_PER_DAY,
          MAX_PLACES_PER_DAY,
          places.size());
    }

    Set<String> seenPlaceNames = new HashSet<>();
    for (int index = 0; index < places.size(); index++) {
      AiPlanResponse.PlaceDetail place = places.get(index);
      if (place == null) {
        fail("NULL_PLACE", "비어 있는 장소 데이터가 포함되어 있습니다. day=%d index=%d", day.day(), index);
      }

      requireText(
          place.name(), "EMPTY_PLACE_NAME", "장소명이 비어 있습니다. day=%d index=%d", day.day(), index);
      requireText(
          place.memo(),
          "EMPTY_PLACE_MEMO",
          "장소 메모가 비어 있습니다. day=%d place=%s",
          day.day(),
          place.name());

      String normalizedName = normalizePlaceName(place.name());
      if (!seenPlaceNames.add(normalizedName)) {
        fail("DUPLICATED_PLACE", "같은 Day에 중복 장소가 있습니다. day=%d place=%s", day.day(), place.name());
      }
    }
  }

  private int calculateExpectedDays(String startDate, String endDate) {
    try {
      LocalDate start = LocalDate.parse(startDate);
      LocalDate end = LocalDate.parse(endDate);
      long days = ChronoUnit.DAYS.between(start, end) + 1;
      if (days <= 0 || days > 30) {
        fail("INVALID_DATE_RANGE", "여행 날짜 범위가 유효하지 않습니다. start=%s end=%s", startDate, endDate);
      }
      return Math.toIntExact(days);
    } catch (BusinessException e) {
      throw e;
    } catch (Exception e) {
      fail("INVALID_DATE_FORMAT", "여행 날짜 형식이 유효하지 않습니다. start=%s end=%s", startDate, endDate);
      return 0;
    }
  }

  private void requireText(String value, String reason, String detail, Object... args) {
    if (value == null || value.isBlank()) {
      fail(reason, detail, args);
    }
  }

  private String normalizePlaceName(String value) {
    return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("\\s+", "").trim();
  }

  private void fail(String reason, String detail, Object... args) {
    String message = String.format(detail, args);
    log.warn("[PLAN-QUALITY] invalid reason={} detail={}", reason, message);
    throw new BusinessException("일정 품질 검사 실패 (" + reason + "): " + message, HttpStatus.BAD_GATEWAY);
  }

  public void validateRequest(com.compass.domain.agent.dto.PlanRequest request) {
    if (request == null || request.startDate() == null || request.endDate() == null) {
      throw new BusinessException("여행 날짜가 필요합니다.", HttpStatus.BAD_REQUEST);
    }
    long days = ChronoUnit.DAYS.between(request.startDate(), request.endDate()) + 1;
    if (request.startDate().isBefore(LocalDate.now(java.time.ZoneId.of("Asia/Seoul")))
        || days < 1
        || days > 7) {
      throw new BusinessException("오늘 이후의 1~7일 여행을 선택해 주세요.", HttpStatus.BAD_REQUEST);
    }
    if (request.seniors() > request.adults())
      throw new BusinessException("어르신 수는 성인 수를 넘을 수 없습니다.", HttpStatus.BAD_REQUEST);
    if (request.companion() == com.compass.domain.agent.dto.PlanRequest.Companion.SOLO
        && (request.adults() != 1 || request.children() != 0)) {
      throw new BusinessException("혼자 여행은 성인 1명으로 설정해 주세요.", HttpStatus.BAD_REQUEST);
    }
    if (days == 1
        && request.arrivalTime() != null
        && request.departureTime() != null
        && !request.arrivalTime().isBefore(request.departureTime())) {
      throw new BusinessException("출발 시각은 도착 시각보다 늦어야 합니다.", HttpStatus.BAD_REQUEST);
    }
    if (new HashSet<>(request.requiredPlaceIds()).size() != request.requiredPlaceIds().size()) {
      throw new BusinessException("필수 장소가 중복됐습니다.", HttpStatus.BAD_REQUEST);
    }
  }

  public void validateAgainstRequest(
      AiPlanResponse plan,
      com.compass.domain.agent.dto.PlanRequest request,
      java.util.Map<String, TravelAgentService.Candidate> candidates) {
    if (!request.startDate().toString().equals(plan.startDate())
        || !request.endDate().toString().equals(plan.endDate())) {
      fail("REQUEST_DATES", "요청한 여행 날짜와 다릅니다.");
    }
    Set<String> used = new HashSet<>();
    if (plan.title().length() > 255) fail("TITLE_LENGTH", "일정 제목이 너무 깁니다.");
    int minimum =
        switch (request.pace()) {
          case RELAXED -> 2;
          case BALANCED -> 3;
          case ACTIVE -> 4;
        };
    int maximum =
        switch (request.pace()) {
          case RELAXED -> 3;
          case BALANCED -> 4;
          case ACTIVE -> 5;
        };
    for (var day : plan.itineraries()) {
      if (day.places().size() < minimum || day.places().size() > maximum)
        fail("PACE", "선택한 여행 강도의 장소 수 범위를 벗어났습니다.");
      int earliest =
          day.day() == 1 && request.arrivalTime() != null
              ? request.arrivalTime().toSecondOfDay() / 60
              : 9 * 60;
      int latest =
          day.day() == plan.itineraries().size() && request.departureTime() != null
              ? request.departureTime().toSecondOfDay() / 60
              : 21 * 60;
      int previousEnd = earliest - 20;
      LocalDate date = request.startDate().plusDays(day.day() - 1L);
      for (var place : day.places()) {
        if (!used.add(place.googlePlaceId())) fail("DUPLICATE_ID", "같은 장소가 여러 번 포함됐습니다.");
        var candidate = candidates.get(place.googlePlaceId());
        if (candidate == null) fail("UNKNOWN_ID", "확인하지 않은 장소가 포함됐습니다.");
        if (place.durationMinutes() == null
            || place.durationMinutes() < 30
            || place.durationMinutes() > 180) fail("DURATION", "체류시간은 30~180분이어야 합니다.");
        int start;
        try {
          start = java.time.LocalTime.parse(place.startTime()).toSecondOfDay() / 60;
        } catch (Exception e) {
          fail("TIME_FORMAT", "방문 시각이 올바르지 않습니다.");
          return;
        }
        int end = start + place.durationMinutes();
        if (start < earliest || start < previousEnd + 20 || end > latest)
          fail("TIME_OVERLAP", "일정 시각이 겹치거나 도착/출발 시각을 벗어납니다.");
        if (!openDuring(candidate.place(), date, start, end)) fail("CLOSED", "영업시간 밖의 방문이 포함됐습니다.");
        previousEnd = end;
        if (!Set.of("관광", "식사", "카페", "쇼핑", "휴식", "체험").contains(place.category()))
          fail("CATEGORY", "지원하지 않는 장소 분류입니다.");
        if (place.memo().length() > 1600) fail("MEMO_LENGTH", "장소 설명이 너무 깁니다.");
      }
    }
    if (!used.containsAll(
        request.requiredPlaceIds().stream().map(PlaceCatalog::normalizeId).toList()))
      fail("REQUIRED", "필수 방문 장소가 빠졌습니다.");
  }

  static boolean openDuring(PlaceCatalog.PlaceInfo place, LocalDate date, int start, int end) {
    var hours = place.regularHours();
    LocalDate now = LocalDate.now(java.time.ZoneId.of("Asia/Seoul"));
    if (!date.isBefore(now)
        && date.isBefore(now.plusDays(7))
        && place.currentHours().path("periods").isArray()) hours = place.currentHours();
    var periods = hours.path("periods");
    if (!periods.isArray() || periods.isEmpty())
      return true; // Missing data is NOT evidence of closure.
    int visit = (date.getDayOfWeek().getValue() % 7) * 1440 + start;
    for (var period : periods) {
      var open = period.path("open");
      var close = period.path("close");
      if (close.isMissingNode() && open.path("day").asInt() == 0 && open.path("hour").asInt() == 0)
        return true;
      if (!open.has("day") || !close.has("day")) return true;
      int from =
          open.path("day").asInt() * 1440
              + open.path("hour").asInt() * 60
              + open.path("minute").asInt();
      int until =
          close.path("day").asInt() * 1440
              + close.path("hour").asInt() * 60
              + close.path("minute").asInt();
      if (until <= from) until += 7 * 1440;
      for (int offset : new int[] {0, 7 * 1440})
        if (visit + offset >= from && visit + offset + end - start <= until) return true;
    }
    return false;
  }
}
