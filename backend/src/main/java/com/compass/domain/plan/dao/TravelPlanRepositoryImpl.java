package com.compass.domain.plan.dao;

import com.compass.domain.plan.domain.QTravelPlan;
import com.compass.domain.plan.domain.QItinerary;
import com.compass.domain.plan.domain.QPlace;
import com.compass.domain.plan.domain.TravelPlan;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

import static com.compass.domain.plan.domain.QTravelPlan.travelPlan;
import static com.compass.domain.plan.domain.QItinerary.itinerary;
import static com.compass.domain.plan.domain.QPlace.place;

@RequiredArgsConstructor
public class TravelPlanRepositoryImpl implements TravelPlanRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<TravelPlan> findByIdWithDetails(Long planId) {
        return Optional.ofNullable(queryFactory
                .selectFrom(travelPlan)
                .leftJoin(travelPlan.itineraries, itinerary).fetchJoin()
                .leftJoin(itinerary.places, place).fetchJoin()
                .where(travelPlan.id.eq(planId))
                .fetchOne());
    }

    @Override
    public List<TravelPlan> findAllByUserIdWithDetails(Long userId) {
        return queryFactory
                .selectFrom(travelPlan)
                .leftJoin(travelPlan.itineraries, itinerary).fetchJoin()
                .leftJoin(itinerary.places, place).fetchJoin()
                .where(travelPlan.user.id.eq(userId))
                .distinct()
                .fetch();
    }
}