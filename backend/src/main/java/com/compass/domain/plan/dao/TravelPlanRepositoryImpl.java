package com.compass.domain.plan.dao;

import com.compass.domain.plan.domain.TravelPlan;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

import static com.compass.domain.plan.domain.QTravelPlan.travelPlan;

// 여행 계획 Querydsl 구현체
@RequiredArgsConstructor
public class TravelPlanRepositoryImpl implements TravelPlanRepositoryCustom {

    private final JPAQueryFactory query;

    @Override
    public List<TravelPlan> findAllByUserIdWithDetails(Long userId) {
        // fetchJoin을 제거하여 MultipleBagFetchException 방지
        // @BatchSize 설정에 의해 itineraries와 places는 필요 시 IN 절로 조회됨
        return query
                .selectFrom(travelPlan)
                .where(travelPlan.user.id.eq(userId))
                .distinct()
                .fetch();
    }

    @Override
    public Optional<TravelPlan> findByIdWithDetails(Long planId) {
        // 단일 조회에서도 조인을 제거하고 루트 엔티티만 조회
        var result = query
                .selectFrom(travelPlan)
                .where(travelPlan.id.eq(planId))
                .fetchOne();

        return Optional.ofNullable(result);
    }
}