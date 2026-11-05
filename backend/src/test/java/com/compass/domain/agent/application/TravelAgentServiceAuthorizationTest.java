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
}
