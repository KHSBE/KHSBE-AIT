package com.compass.domain.plan.api;

import com.compass.domain.plan.application.PlanCommandService;
import com.compass.domain.plan.dao.TravelPlanRepository;
import com.compass.domain.plan.domain.TravelPlan;
import com.compass.domain.user.dao.UserRepository;
import com.compass.domain.user.domain.User;
import com.compass.global.common.ApiResponse;
import com.compass.global.error.BusinessException;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class TravelPlanController {

  private final TravelPlanRepository travelPlanRepository;
  private final PlanCommandService planCommandService;
  private final com.compass.domain.plan.application.PlanEditService planEditService;
  private final UserRepository userRepository;

  @GetMapping
  public ApiResponse<List<TravelPlan>> getMyPlans(Authentication authentication) {
    User user = findUser(authentication);
    return ApiResponse.success(travelPlanRepository.findAllByUserIdWithDetails(user.getId()));
  }

  @GetMapping("/{planId}")
  public ApiResponse<TravelPlan> getPlanDetail(
      @PathVariable Long planId, Authentication authentication) {
    User user = findUser(authentication);
    TravelPlan plan =
        travelPlanRepository
            .findByIdAndUserIdWithDetails(planId, user.getId())
            .orElseThrow(() -> new BusinessException("해당 일정을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
    return ApiResponse.success(plan);
  }

  @DeleteMapping("/{planId}")
  public ApiResponse<Void> deletePlan(@PathVariable Long planId, Authentication authentication) {
    User user = findUser(authentication);
    planCommandService.deletePlan(user, planId);
    return ApiResponse.success(null);
  }

  @PutMapping("/{planId}/places")
  public ApiResponse<TravelPlan> updatePlan(
      @PathVariable Long planId,
      @Valid @RequestBody com.compass.domain.plan.dto.UpdatePlanRequest request,
      Authentication auth) {
    return ApiResponse.success(planEditService.update(findUser(auth), planId, request));
  }

  private User findUser(Authentication authentication) {
    if (authentication == null) {
      throw new BusinessException("인증 정보가 없습니다.", HttpStatus.UNAUTHORIZED);
    }
    String email = authentication.getName();
    return userRepository
        .findByEmail(email)
        .orElseThrow(() -> new BusinessException("사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
  }
}
