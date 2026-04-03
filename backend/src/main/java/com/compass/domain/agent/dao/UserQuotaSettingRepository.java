package com.compass.domain.agent.dao;

import com.compass.domain.agent.domain.UserQuotaSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface UserQuotaSettingRepository extends JpaRepository<UserQuotaSetting, Long> {
    Optional<UserQuotaSetting> findByUserId(Long userId);

    @Modifying
    @Transactional
    @Query("delete from UserQuotaSetting u where u.userId = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);
}
