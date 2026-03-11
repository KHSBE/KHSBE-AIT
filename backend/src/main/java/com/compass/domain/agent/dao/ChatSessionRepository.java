package com.compass.domain.agent.dao;

import com.compass.domain.agent.domain.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
    List<ChatSession> findAllByUserId(Long userId);
}