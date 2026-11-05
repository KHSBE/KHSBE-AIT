package com.compass.domain.agent.application;

import static org.junit.jupiter.api.Assertions.*;

import com.compass.domain.agent.dao.*;
import com.compass.domain.agent.dto.PlanRequest;
import com.compass.domain.user.dao.UserRepository;
import com.compass.domain.user.domain.User;
import com.compass.global.config.QuerydslConfig;
import com.compass.global.error.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.transaction.annotation.*;

@DataJpaTest(
    properties = {
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
      "spring.jpa.show-sql=false",
      "app.quota.plan-per-day=1"
    })
@Import({RequestQuotaService.class, QuerydslConfig.class, RequestQuotaServiceTest.Config.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class RequestQuotaServiceTest {
  @TestConfiguration
  static class Config {
    @Bean
    ObjectMapper mapper() {
      return new ObjectMapper().findAndRegisterModules();
    }
  }

  @Autowired RequestQuotaService quota;
  @Autowired UserRepository users;
  @Autowired PlanGenerationRepository generations;
  Long userId;

  @BeforeEach
  void setup() {
    userId =
        users
            .save(User.builder().email(UUID.randomUUID() + "@test.com").nickname("test").build())
            .getId();
  }

  PlanRequest request(UUID id) {
    var date = LocalDate.now(ZoneId.of("Asia/Seoul")).plusDays(2);
    return new PlanRequest(
        id,
        "ChIJdestination",
        date,
        date,
        null,
        null,
        PlanRequest.Companion.SOLO,
        1,
        0,
        0,
        List.of(PlanRequest.Interest.FOOD),
        PlanRequest.Pace.RELAXED,
        null,
        List.of(),
        "");
  }

  @Test
  void successSurvivesContentDeletionAndReplayDoesNotChargeAgain() {
    var req = request(UUID.randomUUID());
    var ticket = quota.reserve(userId, req);
    quota.complete(userId, ticket, 123L);
    assertEquals(0, quota.getQuotaStatus(userId).planPerDayRemaining());
    assertEquals(123L, quota.reserve(userId, req).existingPlanId());
    assertThrows(BusinessException.class, () -> quota.reserve(userId, request(UUID.randomUUID())));
  }

  @Test
  void failureReleasesReservationAndSameRequestCanRetry() {
    var req = request(UUID.randomUUID());
    var ticket = quota.reserve(userId, req);
    quota.fail(userId, ticket);
    assertEquals(1, quota.getQuotaStatus(userId).planPerDayRemaining());
    assertNotNull(quota.reserve(userId, req).generationId());
  }

  @Test
  void concurrentRequestsCannotBothAcquireLastSlot() throws Exception {
    var pool = Executors.newFixedThreadPool(2);
    var start = new CountDownLatch(1);
    Callable<Boolean> task =
        () -> {
          start.await();
          try {
            quota.reserve(userId, request(UUID.randomUUID()));
            return true;
          } catch (BusinessException e) {
            return false;
          }
        };
    try {
      var a = pool.submit(task);
      var b = pool.submit(task);
      start.countDown();
      assertNotEquals(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS));
      assertEquals(0, quota.getQuotaStatus(userId).planPerDayRemaining());
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void adminRefillDoesNotDiscardPendingReservation() {
    quota.reserve(userId, request(UUID.randomUUID()));
    quota.resetQuotaToDefaultAndRefillToday(userId);
    assertEquals(0, quota.getQuotaStatus(userId).planPerDayRemaining());
  }

  @Test
  void lateWorkerCannotCompleteOrFailANewerAttempt() {
    var request = request(UUID.randomUUID());
    var old = quota.reserve(userId, request);
    var record = generations.findById(old.generationId()).orElseThrow();
    record.setExpiresAt(Instant.now().minusSeconds(1));
    generations.saveAndFlush(record);
    var current = quota.reserve(userId, request);
    assertThrows(BusinessException.class, () -> quota.complete(userId, old, 123L));
    quota.fail(userId, old);
    quota.complete(userId, current, 456L);
    assertEquals(456L, quota.reserve(userId, request).existingPlanId());
  }
}
