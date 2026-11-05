package com.compass.domain.agent.dao;

import com.compass.domain.agent.domain.PlanUsageDay;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanUsageDayRepository extends JpaRepository<PlanUsageDay, Long> {
  Optional<PlanUsageDay> findByUserIdAndQuotaDate(Long userId, LocalDate date);
}
