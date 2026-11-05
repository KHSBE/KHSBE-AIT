package com.compass.domain.agent.application;

import static org.junit.jupiter.api.Assertions.*;

import com.compass.domain.agent.dto.*;
import com.compass.global.error.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class StructuredPlanValidationTest {
  private final ObjectMapper mapper = new ObjectMapper();
  private final PlanQualityValidator validator = new PlanQualityValidator();

  PlanRequest request(LocalDate start, LocalDate end) {
    return new PlanRequest(
        UUID.randomUUID(),
        "ChIJcity",
        start,
        end,
        null,
        null,
        PlanRequest.Companion.SOLO,
        1,
        0,
        0,
        List.of(PlanRequest.Interest.FOOD),
        PlanRequest.Pace.RELAXED,
        null,
        List.of(),
        "");
  }

  @Test
  void rejectsReversedAndExcessiveDateRanges() {
    var date = LocalDate.now().plusDays(5);
    assertThrows(
        BusinessException.class, () -> validator.validateRequest(request(date, date.minusDays(1))));
    assertThrows(
        BusinessException.class, () -> validator.validateRequest(request(date, date.plusDays(7))));
  }

  @Test
  void rejectsWrongCountryAndOutsideViewport() throws Exception {
    var hours = mapper.readTree("{}");
    var city =
        new PlaceCatalog.PlaceInfo(
            "city",
            "city",
            "",
            35,
            129,
            "KR",
            List.of("locality"),
            "",
            34,
            128,
            36,
            130,
            hours,
            hours,
            "");
    assertFalse(
        city.contains(
            new PlaceCatalog.PlaceInfo(
                "p", "p", "", 35, 129, "JP", List.of(), "", 0, 0, 0, 0, hours, hours, "")));
    assertFalse(
        city.contains(
            new PlaceCatalog.PlaceInfo(
                "p", "p", "", 37, 129, "KR", List.of(), "", 0, 0, 0, 0, hours, hours, "")));
  }

  @Test
  void checksOvernightHoursAndUnknownData() throws Exception {
    var h =
        mapper.readTree(
            "{\"periods\":[{\"open\":{\"day\":1,\"hour\":22},\"close\":{\"day\":2,\"hour\":2}}]}");
    var missing = mapper.readTree("{}");
    var p =
        new PlaceCatalog.PlaceInfo(
            "p", "p", "", 0, 0, "KR", List.of(), "OPERATIONAL", 0, 0, 0, 0, h, missing, "");
    assertTrue(PlanQualityValidator.openDuring(p, LocalDate.of(2030, 1, 8), 30, 60));
    assertFalse(PlanQualityValidator.openDuring(p, LocalDate.of(2030, 1, 8), 180, 240));
    var unknown =
        new PlaceCatalog.PlaceInfo(
            "p", "p", "", 0, 0, "KR", List.of(), "", 0, 0, 0, 0, missing, missing, "");
    assertTrue(PlanQualityValidator.openDuring(unknown, LocalDate.of(2030, 1, 8), 0, 60));
  }
}
