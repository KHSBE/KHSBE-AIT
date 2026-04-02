package com.compass.domain.plan.dao;

import com.compass.domain.plan.domain.TravelPlan;

import java.util.List;
import java.util.Optional;

public interface TravelPlanRepositoryCustom {
    Optional<TravelPlan> findByIdWithDetails(Long planId);

    Optional<TravelPlan> findByIdAndUserIdWithDetails(Long planId, Long userId);

    List<TravelPlan> findAllByUserIdWithDetails(Long userId);
}

