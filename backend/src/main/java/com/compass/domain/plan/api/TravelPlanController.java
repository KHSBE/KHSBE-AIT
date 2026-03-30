package com.compass.domain.plan.api;

import com.compass.domain.plan.dao.TravelPlanRepository;
import com.compass.domain.plan.domain.TravelPlan;
import com.compass.domain.user.dao.UserRepository;
import com.compass.domain.user.domain.User;
import com.compass.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class TravelPlanController {

    private final TravelPlanRepository travelPlanRepository;
    private final UserRepository userRepository;

    @GetMapping
    public ApiResponse<List<TravelPlan>> getMyPlans(Authentication authentication) {
        User user = findUser(authentication);
        return ApiResponse.success(travelPlanRepository.findAllByUserIdWithDetails(user.getId()));
    }

    @GetMapping("/{planId}")
    public ApiResponse<TravelPlan> getPlanDetail(@PathVariable Long planId) {
        TravelPlan plan = travelPlanRepository.findByIdWithDetails(planId)
                .orElseThrow(() -> new RuntimeException("해당 계획을 찾을 수 없습니다."));
        return ApiResponse.success(plan);
    }

    @DeleteMapping("/{planId}")
    public ApiResponse<Void> deletePlan(@PathVariable Long planId) {
        travelPlanRepository.deleteById(planId);
        return ApiResponse.success(null);
    }

    private User findUser(Authentication authentication) {
        if (authentication == null) throw new RuntimeException("인증 정보가 없습니다.");
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
    }
}