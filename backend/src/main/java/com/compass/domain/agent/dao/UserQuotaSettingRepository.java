package com.compass.domain.agent.dao;

import com.compass.domain.agent.domain.UserQuotaSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserQuotaSettingRepository extends JpaRepository<UserQuotaSetting, Long> {
    Optional<UserQuotaSetting> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
