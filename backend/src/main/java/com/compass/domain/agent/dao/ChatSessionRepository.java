package com.compass.domain.agent.dao;

import com.compass.domain.agent.domain.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    // 유저의 세션 목록을 최신순으로 조회하며, 연결된 여행 계획을 Join Fetch로 가져옴
    @Query("SELECT s FROM ChatSession s LEFT JOIN FETCH s.travelPlan WHERE s.user.id = :userId ORDER BY s.createdAt DESC")
    List<ChatSession> findAllByUserIdWithPlan(@Param("userId") Long userId);
}