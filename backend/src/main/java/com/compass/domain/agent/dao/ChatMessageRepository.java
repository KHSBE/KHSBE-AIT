package com.compass.domain.agent.dao;

import com.compass.domain.agent.domain.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    // 세션별 메시지 내역을 시간순으로 조회
    List<ChatMessage> findAllBySessionIdOrderBySentAtAsc(Long sessionId);
}