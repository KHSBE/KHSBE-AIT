package com.compass.domain.agent.dao;

import com.compass.domain.agent.domain.PlanGeneration;
import java.time.*;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanGenerationRepository extends JpaRepository<PlanGeneration, Long> {
  Optional<PlanGeneration> findByUserIdAndRequestId(Long userId, String requestId);

  long countByUserIdAndQuotaDateAndStatusAndExpiresAtAfter(
      Long userId, LocalDate date, String status, Instant now);

  long countByUserIdAndStatusAndExpiresAtAfter(Long userId, String status, Instant now);
}
