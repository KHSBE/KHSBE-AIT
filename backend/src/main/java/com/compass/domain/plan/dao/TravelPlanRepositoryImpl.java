package com.compass.domain.plan.dao;

import com.compass.domain.plan.domain.TravelPlan;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

import static com.compass.domain.plan.domain.QTravelPlan.travelPlan;

@RequiredArgsConstructor
public class TravelPlanRepositoryImpl implements TravelPlanRepositoryCustom {

    private final JPAQueryFactory query;

    @Override
    public List<TravelPlan> findAllByUserIdWithDetails(Long userId) {
        return query
                .selectFrom(travelPlan)
                .where(travelPlan.user.id.eq(userId))
                .distinct()
                .fetch();
    }

    @Override
    public Optional<TravelPlan> findByIdWithDetails(Long planId) {
        var result = query
                .selectFrom(travelPlan)
                .where(travelPlan.id.eq(planId))
                .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    public Optional<TravelPlan> findByIdAndUserIdWithDetails(Long planId, Long userId) {
        var result = query
                .selectFrom(travelPlan)
                .where(
                        travelPlan.id.eq(planId),
                        travelPlan.user.id.eq(userId)
                )
                .fetchOne();

        return Optional.ofNullable(result);
    }
}

