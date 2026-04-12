package com.compass.domain.agent.application;

import com.compass.domain.agent.dto.AiPlanResponse;
import com.compass.global.error.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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
                    days.size()
            );
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
                        day.day()
                );
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
                    totalPlaces
            );
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
                    places.size()
            );
        }

        Set<String> seenPlaceNames = new HashSet<>();
        for (int index = 0; index < places.size(); index++) {
            AiPlanResponse.PlaceDetail place = places.get(index);
            if (place == null) {
                fail("NULL_PLACE", "비어 있는 장소 데이터가 포함되어 있습니다. day=%d index=%d", day.day(), index);
            }

            requireText(place.name(), "EMPTY_PLACE_NAME", "장소명이 비어 있습니다. day=%d index=%d", day.day(), index);
            requireText(place.memo(), "EMPTY_PLACE_MEMO", "장소 메모가 비어 있습니다. day=%d place=%s", day.day(), place.name());

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
        throw new BusinessException("AI가 만든 일정이 품질 기준을 통과하지 못했습니다. 다시 시도해 주세요.", HttpStatus.BAD_GATEWAY);
    }
}
