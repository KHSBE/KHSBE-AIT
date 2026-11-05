package com.compass.domain.agent.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.compass.domain.agent.api.TravelAgentController;
import com.compass.domain.plan.application.PlanGenerationPersistence;
import com.compass.domain.plan.dao.TravelPlanRepository;
import com.compass.domain.user.dao.UserRepository;
import com.compass.global.error.BusinessException;
import org.junit.jupiter.api.Test;

class TravelAgentServiceAuthorizationTest {
  @Test
  void anonymousCannotCreateOrSpendQuota() {
    var generator = mock(TravelAgentService.class);
    var quota = mock(RequestQuotaService.class);
    var controller =
        new TravelAgentController(
            generator,
            mock(PlanQualityValidator.class),
            quota,
            mock(PlanGenerationPersistence.class),
            mock(TravelPlanRepository.class),
            mock(UserRepository.class));
    assertThrows(BusinessException.class, () -> controller.createPlan(null, null));
    verifyNoInteractions(generator, quota);
  }

  @Test
  void providerTimeoutReleasesQuotaWithoutSavingOrCharging() {
    var generator = mock(TravelAgentService.class);
    var validator = mock(PlanQualityValidator.class);
    var quota = mock(RequestQuotaService.class);
    var persistence = mock(PlanGenerationPersistence.class);
    var plans = mock(TravelPlanRepository.class);
    var users = mock(UserRepository.class);
    var controller =
        new TravelAgentController(generator, validator, quota, persistence, plans, users);
    var user =
        com.compass.domain.user.domain.User.builder().id(1L).email("test@example.com").build();
    var date = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Seoul")).plusDays(20);
    var request =
        new com.compass.domain.agent.dto.PlanRequest(
            java.util.UUID.randomUUID(),
            "city-123",
            date,
            date,
            null,
            null,
            com.compass.domain.agent.dto.PlanRequest.Companion.SOLO,
            1,
            0,
            0,
            java.util.List.of(com.compass.domain.agent.dto.PlanRequest.Interest.FOOD),
            com.compass.domain.agent.dto.PlanRequest.Pace.RELAXED,
            null,
            java.util.List.of(),
            "");
    var ticket = new RequestQuotaService.Ticket(77L, null, 1);
    when(users.findByEmail(user.getEmail())).thenReturn(java.util.Optional.of(user));
    when(quota.reserve(1L, request)).thenReturn(ticket);
    var failure =
        new AiUnavailableException("timeout", org.springframework.http.HttpStatus.GATEWAY_TIMEOUT);
    when(generator.generate(request)).thenThrow(failure);
    var auth =
        new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
            user.getEmail(), "", java.util.List.of());
    assertSame(
        failure,
        assertThrows(AiUnavailableException.class, () -> controller.createPlan(request, auth)));
    verify(quota).fail(1L, ticket);
    verify(quota, never()).complete(anyLong(), any(), anyLong());
    verifyNoInteractions(persistence, plans);
  }
}
