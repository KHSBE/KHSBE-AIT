package com.compass.domain.agent.application;

import com.compass.domain.agent.dao.ChatMessageRepository;
import com.compass.domain.plan.dao.TravelPlanRepository;
import com.compass.global.error.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RequestQuotaService {

    private final TravelPlanRepository travelPlanRepository;
    private final ChatMessageRepository chatMessageRepository;

    @Value("${app.quota.plan-per-day:3}")
    private int planPerDayLimit;

    @Value("${app.quota.chat-per-session:10}")
    private int chatPerSessionLimit;

    public void assertPlanCreationAllowed(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();
        long used = travelPlanRepository.countByUserIdAndCreatedAtBetween(userId, start, end);

        if (used >= Math.max(1, planPerDayLimit)) {
            throw new BusinessException(
                    "하루 일정 생성 한도(" + planPerDayLimit + "회)를 초과했습니다.",
                    HttpStatus.TOO_MANY_REQUESTS
            );
        }
    }

    public void assertChatAllowedInSession(Long sessionId) {
        long used = chatMessageRepository.countBySessionIdAndSenderRole(sessionId, "USER");

        if (used >= Math.max(1, chatPerSessionLimit)) {
            throw new BusinessException(
                    "이 채팅 세션의 메시지 한도(" + chatPerSessionLimit + "회)를 초과했습니다.",
                    HttpStatus.TOO_MANY_REQUESTS
            );
        }
    }
}
