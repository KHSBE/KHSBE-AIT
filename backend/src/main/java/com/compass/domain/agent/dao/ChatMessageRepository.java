package com.compass.domain.agent.dao;

import com.compass.domain.agent.domain.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findAllBySessionIdOrderBySentAtAsc(Long sessionId);

    List<ChatMessage> findAllBySessionIdAndSessionUserIdOrderBySentAtAsc(Long sessionId, Long userId);

    long countBySessionIdAndSenderRole(Long sessionId, String senderRole);

    long countBySessionUserIdAndSenderRoleAndSentAtBetween(
            Long userId,
            String senderRole,
            LocalDateTime startInclusive,
            LocalDateTime endExclusive
    );
}
