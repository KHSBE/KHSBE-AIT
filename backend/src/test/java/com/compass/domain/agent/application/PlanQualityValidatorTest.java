package com.compass.domain.agent.application;

import com.compass.domain.agent.dto.AiPlanResponse;
import com.compass.global.error.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlanQualityValidatorTest {

    private PlanQualityValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PlanQualityValidator();
    }

    @Test
    void validate_shouldPass_whenPlanMeetsMinimumQuality() {
        AiPlanResponse plan = plan(
                "부산 해운대 2일 여행",
                "부산 해운대",
                "2026-04-10",
                "2026-04-11",
                List.of(
                        day(1, "해운대 산책과 맛집", places("해운대 해수욕장", "해리단길")),
                        day(2, "시장과 귀가 동선", places("자갈치시장", "부산역"))
                )
        );

        assertDoesNotThrow(() -> validator.validate(plan));
    }

    @Test
    void validate_shouldThrow502_whenDayCountDoesNotMatchDateRange() {
        AiPlanResponse plan = plan(
                "부산 해운대 2일 여행",
                "부산 해운대",
                "2026-04-10",
                "2026-04-11",
                List.of(day(1, "해운대 산책", places("해운대 해수욕장", "동백섬")))
        );

        BusinessException exception = assertThrows(BusinessException.class, () -> validator.validate(plan));

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getHttpStatus());
    }

    @Test
    void validate_shouldThrow502_whenDayNumberIsOutOfOrder() {
        AiPlanResponse plan = plan(
                "부산 해운대 2일 여행",
                "부산 해운대",
                "2026-04-10",
                "2026-04-11",
                List.of(
                        day(1, "해운대 산책", places("해운대 해수욕장", "동백섬")),
                        day(3, "시장과 귀가", places("자갈치시장", "부산역"))
                )
        );

        BusinessException exception = assertThrows(BusinessException.class, () -> validator.validate(plan));

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getHttpStatus());
    }

    @Test
    void validate_shouldThrow502_whenPlaceNameIsBlank() {
        AiPlanResponse plan = plan(
                "부산 해운대 1일 여행",
                "부산 해운대",
                "2026-04-10",
                "2026-04-10",
                List.of(day(1, "해운대 산책", List.of(
                        place("해운대 해수욕장"),
                        place(" ")
                )))
        );

        BusinessException exception = assertThrows(BusinessException.class, () -> validator.validate(plan));

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getHttpStatus());
    }

    @Test
    void validate_shouldThrow502_whenPlaceCountExceedsLimit() {
        List<AiPlanResponse.PlaceDetail> places = new ArrayList<>();
        for (int i = 1; i <= 7; i++) {
            places.add(place("장소 " + i));
        }

        AiPlanResponse plan = plan(
                "부산 해운대 1일 여행",
                "부산 해운대",
                "2026-04-10",
                "2026-04-10",
                List.of(day(1, "과도한 일정", places))
        );

        BusinessException exception = assertThrows(BusinessException.class, () -> validator.validate(plan));

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getHttpStatus());
    }

    @Test
    void validate_shouldThrow502_whenSameDayHasDuplicatedPlace() {
        AiPlanResponse plan = plan(
                "부산 해운대 1일 여행",
                "부산 해운대",
                "2026-04-10",
                "2026-04-10",
                List.of(day(1, "중복 장소", List.of(
                        place("해운대 해수욕장"),
                        place("해운대해수욕장")
                )))
        );

        BusinessException exception = assertThrows(BusinessException.class, () -> validator.validate(plan));

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getHttpStatus());
    }

    private AiPlanResponse plan(
            String title,
            String region,
            String startDate,
            String endDate,
            List<AiPlanResponse.DayItinerary> itineraries
    ) {
        return new AiPlanResponse(title, region, "신라스테이 해운대", startDate, endDate, itineraries);
    }

    private AiPlanResponse.DayItinerary day(int day, String theme, List<AiPlanResponse.PlaceDetail> places) {
        return new AiPlanResponse.DayItinerary(day, theme, places);
    }

    private List<AiPlanResponse.PlaceDetail> places(String... names) {
        List<AiPlanResponse.PlaceDetail> places = new ArrayList<>();
        for (String name : names) {
            places.add(place(name));
        }
        return places;
    }

    private AiPlanResponse.PlaceDetail place(String name) {
        return new AiPlanResponse.PlaceDetail(
                name,
                "오전 방문 추천. 실제 여행자가 참고할 수 있는 메모입니다.",
                null,
                null,
                null
        );
    }
}
