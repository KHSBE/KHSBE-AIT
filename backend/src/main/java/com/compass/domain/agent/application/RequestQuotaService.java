package com.compass.domain.agent.application;

import com.compass.domain.agent.dao.ChatMessageRepository;
import com.compass.domain.agent.dto.QuotaStatusResponse;
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
        long used = getPlanUsedToday(userId);
        int planLimit = getSafePlanLimit();

        if (used >= planLimit) {
            throw new BusinessException(
                    "하루 일정 생성 한도(" + planLimit + "회)를 초과했습니다.",
                    HttpStatus.TOO_MANY_REQUESTS
            );
        }
    }

    public void assertChatAllowedInSession(Long sessionId) {
        long used = getChatUsedInSession(sessionId);
        int chatLimit = getSafeChatLimit();

        if (used >= chatLimit) {
            throw new BusinessException(
                    "이 채팅 세션의 메시지 한도(" + chatLimit + "회)를 초과했습니다.",
                    HttpStatus.TOO_MANY_REQUESTS
            );
        }
    }

    public QuotaStatusResponse getQuotaStatus(Long userId, Long sessionId) {
        int planLimit = getSafePlanLimit();
        int chatLimit = getSafeChatLimit();

        long planUsed = getPlanUsedToday(userId);
        long chatUsed = (sessionId == null) ? 0 : getChatUsedInSession(sessionId);

        return new QuotaStatusResponse(
                planLimit,
                (int) planUsed,
                Math.max(0, planLimit - (int) planUsed),
                chatLimit,
                (int) chatUsed,
                Math.max(0, chatLimit - (int) chatUsed)
        );
    }

    private long getPlanUsedToday(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();
        return travelPlanRepository.countByUserIdAndCreatedAtBetween(userId, start, end);
    }

    private long getChatUsedInSession(Long sessionId) {
        return chatMessageRepository.countBySessionIdAndSenderRole(sessionId, "USER");
    }

    private int getSafePlanLimit() {
        return Math.max(1, planPerDayLimit);
    }

    private int getSafeChatLimit() {
        return Math.max(1, chatPerSessionLimit);
    }
}
