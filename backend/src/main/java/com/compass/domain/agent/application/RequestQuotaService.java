package com.compass.domain.agent.application;

import com.compass.domain.agent.dao.ChatMessageRepository;
import com.compass.domain.agent.dao.UserQuotaSettingRepository;
import com.compass.domain.agent.domain.UserQuotaSetting;
import com.compass.domain.agent.dto.QuotaStatusResponse;
import com.compass.domain.plan.dao.TravelPlanRepository;
import com.compass.global.error.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RequestQuotaService {

    private final TravelPlanRepository travelPlanRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserQuotaSettingRepository userQuotaSettingRepository;

    @Value("${app.quota.plan-per-day:3}")
    private int planPerDayLimit;

    @Value("${app.quota.chat-per-day:10}")
    private int chatPerDayLimit;

    public void assertPlanCreationAllowed(Long userId) {
        QuotaAdminDetail detail = getQuotaAdminDetail(userId);
        if (detail.planPerDayUsed() >= detail.effectivePlanPerDayLimit()) {
            throw new BusinessException(
                    "하루 일정 생성 한도(" + detail.effectivePlanPerDayLimit() + "회)를 초과했습니다.",
                    HttpStatus.TOO_MANY_REQUESTS
            );
        }
    }

    public void assertChatAllowedToday(Long userId) {
        QuotaAdminDetail detail = getQuotaAdminDetail(userId);
        if (detail.chatPerDayUsed() >= detail.effectiveChatPerDayLimit()) {
            throw new BusinessException(
                    "하루 채팅 한도(" + detail.effectiveChatPerDayLimit() + "회)를 초과했습니다.",
                    HttpStatus.TOO_MANY_REQUESTS
            );
        }
    }

    public QuotaStatusResponse getQuotaStatus(Long userId) {
        QuotaAdminDetail detail = getQuotaAdminDetail(userId);
        return new QuotaStatusResponse(
                detail.effectivePlanPerDayLimit(),
                detail.planPerDayUsed(),
                detail.planPerDayRemaining(),
                detail.effectiveChatPerDayLimit(),
                detail.chatPerDayUsed(),
                detail.chatPerDayRemaining()
        );
    }

    public QuotaAdminDetail getQuotaAdminDetail(Long userId) {
        UserQuotaSetting setting = userQuotaSettingRepository.findByUserId(userId).orElse(null);

        int defaultPlanLimit = getDefaultPlanLimit();
        int defaultChatLimit = getDefaultChatLimit();
        Integer overridePlanLimit = setting != null ? normalizeOverride(setting.getPlanPerDayLimit()) : null;
        Integer overrideChatLimit = setting != null ? normalizeOverride(setting.getChatPerDayLimit()) : null;

        int effectivePlanLimit = overridePlanLimit != null ? overridePlanLimit : defaultPlanLimit;
        int effectiveChatLimit = overrideChatLimit != null ? overrideChatLimit : defaultChatLimit;

        int planUsed = (int) getPlanUsedToday(userId);
        int chatUsed = (int) getChatUsedToday(userId);

        return new QuotaAdminDetail(
                defaultPlanLimit,
                defaultChatLimit,
                overridePlanLimit,
                overrideChatLimit,
                effectivePlanLimit,
                effectiveChatLimit,
                planUsed,
                Math.max(0, effectivePlanLimit - planUsed),
                chatUsed,
                Math.max(0, effectiveChatLimit - chatUsed)
        );
    }

    @Transactional
    public void upsertUserQuotaLimit(Long userId, Integer planPerDayLimit, Integer chatPerDayLimit) {
        Integer normalizedPlan = normalizeOverride(planPerDayLimit);
        Integer normalizedChat = normalizeOverride(chatPerDayLimit);

        if (normalizedPlan == null && normalizedChat == null) {
            throw new BusinessException("사용자 쿼터 값이 비어 있습니다.", HttpStatus.BAD_REQUEST);
        }

        UserQuotaSetting setting = userQuotaSettingRepository.findByUserId(userId)
                .orElseGet(() -> UserQuotaSetting.builder().userId(userId).build());

        setting.update(normalizedPlan, normalizedChat);
        userQuotaSettingRepository.save(setting);
    }

    @Transactional
    public void clearUserQuotaLimit(Long userId) {
        userQuotaSettingRepository.deleteAllByUserId(userId);
    }

    private int getDefaultPlanLimit() {
        return Math.max(1, planPerDayLimit);
    }

    private int getDefaultChatLimit() {
        return Math.max(1, chatPerDayLimit);
    }

    private Integer normalizeOverride(Integer value) {
        if (value == null) {
            return null;
        }
        if (value < 1) {
            throw new BusinessException("쿼터 값은 1 이상이어야 합니다.", HttpStatus.BAD_REQUEST);
        }
        return value;
    }

    private long getPlanUsedToday(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();
        return travelPlanRepository.countByUserIdAndCreatedAtBetween(userId, start, end);
    }

    private long getChatUsedToday(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();
        return chatMessageRepository.countBySessionUserIdAndSenderRoleAndSentAtBetween(userId, "USER", start, end);
    }

    public record QuotaAdminDetail(
            int defaultPlanPerDayLimit,
            int defaultChatPerDayLimit,
            Integer overridePlanPerDayLimit,
            Integer overrideChatPerDayLimit,
            int effectivePlanPerDayLimit,
            int effectiveChatPerDayLimit,
            int planPerDayUsed,
            int planPerDayRemaining,
            int chatPerDayUsed,
            int chatPerDayRemaining
    ) {
    }
}
