package com.compass.domain.agent.dao;

import com.compass.domain.agent.domain.UserQuotaDailyReset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface UserQuotaDailyResetRepository extends JpaRepository<UserQuotaDailyReset, Long> {
    Optional<UserQuotaDailyReset> findByUserIdAndQuotaDate(Long userId, LocalDate quotaDate);
}
