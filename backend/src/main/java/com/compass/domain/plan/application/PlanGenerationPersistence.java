package com.compass.domain.plan.application;

import com.compass.domain.agent.application.RequestQuotaService;
import com.compass.domain.agent.application.TravelAgentService;
import com.compass.domain.plan.dao.TravelPlanRepository;
import com.compass.domain.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlanGenerationPersistence {
  private final PlanCommandService commands;
  private final TravelPlanRepository plans;
  private final RequestQuotaService quota;

  @Transactional
  public Long save(
      User user, TravelAgentService.GeneratedPlan generated, RequestQuotaService.Ticket ticket) {
    Long id = commands.savePlanFromAi(user, generated.plan(), generated.accommodation());
    var plan = plans.findByIdAndUserIdWithDetails(id, user.getId()).orElseThrow();
    plan.setCompanion(generated.request().companion().name());
    plan.setStyle(generated.request().interests().toString());
    plan.setDestinationPlaceId(generated.request().destinationPlaceId());
    plan.setGenerationNotes(
        "실제 장소와 지역을 확인한 AI 초안입니다. 임시 휴무·예약 여부는 방문 전에 확인하세요. 장소 사이 20분은 계획 여유이며 실제 이동시간을 계산한 값이"
            + " 아닙니다.");
    for (var day : plan.getItineraries())
      for (var place : day.getPlaces()) {
        var candidate = generated.candidates().get(place.getGooglePlaceId());
        if (candidate != null) {
          place.setSourceUri(candidate.sourceUri());
          place.setSourceTitle(candidate.sourceTitle());
        }
      }
    quota.complete(user.getId(), ticket, id);
    return id;
  }
}
