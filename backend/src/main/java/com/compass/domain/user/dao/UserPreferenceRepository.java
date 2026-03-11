package com.compass.domain.user.dao;

import com.compass.domain.user.domain.UserPreference;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPreferenceRepository extends JpaRepository<UserPreference, Long> {
    // 특정 유저의 ID로 취향 정보를 찾고 싶을 때
    void deleteByUserId(Long userId);
}