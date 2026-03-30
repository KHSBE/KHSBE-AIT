package com.compass.domain.plan.dao;

import com.compass.domain.plan.domain.TravelPlan;
import java.util.List;
import java.util.Optional;

public interface TravelPlanRepositoryCustom {
    // 특정 일정 하나를 상세히 가져오기 (일차별 장소 포함)
    Optional<TravelPlan> findByIdWithDetails(Long planId);

    // 유저의 모든 일정을 가져오기
    List<TravelPlan> findAllByUserIdWithDetails(Long userId);
}