package com.compass.domain.agent.application;

import com.compass.domain.agent.dao.ChatMessageRepository;
import com.compass.domain.agent.dao.UserQuotaDailyResetRepository;
import com.compass.domain.agent.dao.UserQuotaSettingRepository;
import com.compass.domain.plan.dao.TravelPlanRepository;
import com.compass.global.error.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestQuotaServiceTest {

    @Mock
    private TravelPlanRepository travelPlanRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private UserQuotaSettingRepository userQuotaSettingRepository;

    @Mock
    private UserQuotaDailyResetRepository userQuotaDailyResetRepository;

    private RequestQuotaService requestQuotaService;

    @BeforeEach
    void setUp() {
        requestQuotaService = new RequestQuotaService(
                travelPlanRepository,
                chatMessageRepository,
                userQuotaSettingRepository,
                userQuotaDailyResetRepository
        );
        when(userQuotaSettingRepository.findByUserId(anyLong())).thenReturn(java.util.Optional.empty());
        when(userQuotaDailyResetRepository.findByUserIdAndQuotaDate(anyLong(), any())).thenReturn(java.util.Optional.empty());
        ReflectionTestUtils.setField(requestQuotaService, "planPerDayLimit", 3);
        ReflectionTestUtils.setField(requestQuotaService, "chatPerDayLimit", 10);
    }

    @Test
    void assertPlanCreationAllowed_shouldPass_whenUnderLimit() {
        when(travelPlanRepository.countByUserIdAndCreatedAtBetween(any(), any(), any())).thenReturn(2L);
        assertDoesNotThrow(() -> requestQuotaService.assertPlanCreationAllowed(1L));
    }

    @Test
    void assertPlanCreationAllowed_shouldThrow429_whenReachedLimit() {
        when(travelPlanRepository.countByUserIdAndCreatedAtBetween(any(), any(), any())).thenReturn(3L);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> requestQuotaService.assertPlanCreationAllowed(1L)
        );

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exception.getHttpStatus());
    }

    @Test
    void assertChatAllowedToday_shouldThrow429_whenReachedLimit() {
        when(chatMessageRepository.countBySessionUserIdAndSenderRoleAndSentAtBetween(any(), anyString(), any(), any()))
                .thenReturn(10L);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> requestQuotaService.assertChatAllowedToday(11L)
        );

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exception.getHttpStatus());
    }
}
