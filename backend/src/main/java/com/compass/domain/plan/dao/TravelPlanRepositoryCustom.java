package com.compass.domain.plan.dao;

import com.compass.domain.plan.domain.TravelPlan;
import java.util.List;
import java.util.Optional;

public interface TravelPlanRepositoryCustom {
    // 모든 일정과 장소를 한 번에 긁어오는 메서드
    Optional<TravelPlan> findByIdWithDetails(Long planId);
    List<TravelPlan> findAllByUserIdWithDetails(Long userId);
}