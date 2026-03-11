package com.compass.domain.plan.dao;

import com.compass.domain.plan.domain.Itinerary;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItineraryRepository extends JpaRepository<Itinerary, Long> {
}
