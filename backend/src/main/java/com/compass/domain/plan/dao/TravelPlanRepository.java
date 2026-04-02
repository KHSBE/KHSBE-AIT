package com.compass.domain.plan.dao;

import com.compass.domain.plan.domain.TravelPlan;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TravelPlanRepository extends JpaRepository<TravelPlan, Long>, TravelPlanRepositoryCustom {
    long deleteByIdAndUserId(Long id, Long userId);
}

