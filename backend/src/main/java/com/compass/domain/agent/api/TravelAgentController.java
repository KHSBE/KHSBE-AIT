package com.compass.domain.agent.api;

import com.compass.domain.agent.application.*;
import com.compass.domain.agent.dto.PlanRequest;
import com.compass.domain.plan.application.PlanGenerationPersistence;
import com.compass.domain.plan.dao.TravelPlanRepository;
import com.compass.domain.user.dao.UserRepository;
import com.compass.global.common.ApiResponse;
import com.compass.global.error.BusinessException;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class TravelAgentController {
  private final TravelAgentService generator;
  private final PlanQualityValidator validator;
  private final RequestQuotaService quota;
  private final PlanGenerationPersistence persistence;
  private final TravelPlanRepository plans;
  private final UserRepository users;

  @PostMapping("/plan")
  public ApiResponse<Map<String, Long>> createPlan(
      @Valid @RequestBody PlanRequest request, Authentication auth) {
    if (auth == null) throw new BusinessException("로그인이 필요합니다.", HttpStatus.UNAUTHORIZED);
    var user =
        users
            .findByEmail(auth.getName())
            .orElseThrow(() -> new BusinessException("사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
    validator.validateRequest(request);
    var ticket = quota.reserve(user.getId(), request);
    if (ticket.existingPlanId() != null) {
      plans
          .findByIdAndUserIdWithDetails(ticket.existingPlanId(), user.getId())
          .orElseThrow(
              () -> new BusinessException("이미 생성한 일정이 삭제됐습니다. 새 요청으로 생성해 주세요.", HttpStatus.GONE));
      return ApiResponse.success(Map.of("planId", ticket.existingPlanId()));
    }
    try {
      var generated = generator.generate(request);
      Long id = persistence.save(user, generated, ticket);
      return ApiResponse.success(Map.of("planId", id));
    } catch (RuntimeException e) {
      quota.fail(user.getId(), ticket);
      throw e;
    }
  }
}
