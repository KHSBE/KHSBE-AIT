package com.compass.domain.plan.dao;

import com.compass.domain.plan.domain.Place;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaceRepository extends JpaRepository<Place, Long> {
    void deleteByItineraryId(Long itineraryId);
}
