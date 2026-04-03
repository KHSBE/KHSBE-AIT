package com.compass.domain.plan.dao;

import com.compass.domain.plan.domain.TravelPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface TravelPlanRepository extends JpaRepository<TravelPlan, Long>, TravelPlanRepositoryCustom {
    long deleteByIdAndUserId(Long id, Long userId);

    long countByUserIdAndCreatedAtBetween(Long userId, LocalDateTime startInclusive, LocalDateTime endExclusive);
}
