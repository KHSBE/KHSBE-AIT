package com.compass.domain.agent.dto;

import java.util.List;

public record PlanDraft(String title, List<Day> days) {
  public record Day(int day, String theme, List<Visit> places) {}

  public record Visit(
      String placeId, String startTime, int durationMinutes, String category, String memo) {}
}
