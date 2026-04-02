package com.compass.domain.agent.dao;

import com.compass.domain.agent.domain.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    @Query("SELECT s FROM ChatSession s LEFT JOIN FETCH s.travelPlan WHERE s.user.id = :userId ORDER BY s.createdAt DESC")
    List<ChatSession> findAllByUserIdWithPlan(@Param("userId") Long userId);

    Optional<ChatSession> findByIdAndUserId(Long id, Long userId);

    long deleteByIdAndUserId(Long id, Long userId);
}

