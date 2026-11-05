package com.compass.domain.agent.application;

import com.compass.domain.agent.dao.*;
import com.compass.domain.agent.domain.*;
import com.compass.domain.agent.dto.*;
import com.compass.domain.plan.dao.TravelPlanRepository;
import com.compass.domain.user.dao.UserRepository;
import com.compass.global.error.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.MessageDigest;
import java.time.*;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RequestQuotaService {
  private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
  private final PlanGenerationRepository generations;
  private final PlanUsageDayRepository usage;
  private final UserQuotaSettingRepository settings;
  private final UserQuotaDailyResetRepository resets;
  private final TravelPlanRepository plans;
  private final UserRepository users;
  private final ObjectMapper mapper;

  @Value("${app.quota.plan-per-day:3}")
  private int planPerDayLimit;

  @Transactional
  public Ticket reserve(Long userId, PlanRequest request) {
    lock(userId);
    String hash;
    try {
      hash =
          HexFormat.of()
              .formatHex(
                  MessageDigest.getInstance("SHA-256").digest(mapper.writeValueAsBytes(request)));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
    var existing =
        generations.findByUserIdAndRequestId(userId, request.requestId().toString()).orElse(null);
    Instant now = Instant.now();
    if (existing != null) {
      if (!hash.equals(existing.getFingerprint()))
        throw new BusinessException("같은 요청 ID의 조건을 변경할 수 없습니다.", HttpStatus.CONFLICT);
      if ("SUCCEEDED".equals(existing.getStatus()))
        return new Ticket(existing.getId(), existing.getPlanId(), existing.getAttempt());
      if ("PENDING".equals(existing.getStatus()) && existing.getExpiresAt().isAfter(now))
        throw new BusinessException("이미 일정을 생성하고 있습니다. 잠시 기다려 주세요.", HttpStatus.CONFLICT);
    }
    LocalDate today = LocalDate.now(ZONE);
    PlanUsageDay day = day(userId, today);
    long active =
        generations.countByUserIdAndQuotaDateAndStatusAndExpiresAtAfter(
            userId, today, "PENDING", now);
    if (day.getUsed() + active >= limit(userId))
      throw new BusinessException("하루 일정 생성 한도를 초과했습니다.", HttpStatus.TOO_MANY_REQUESTS);
    if (generations.countByUserIdAndStatusAndExpiresAtAfter(userId, "PENDING", now) > 0)
      throw new BusinessException("진행 중인 일정 생성이 있습니다.", HttpStatus.CONFLICT);
    var record = existing == null ? new PlanGeneration() : existing;
    record.setUserId(userId);
    record.setRequestId(request.requestId().toString());
    record.setFingerprint(hash);
    record.setQuotaDate(today);
    record.setStatus("PENDING");
    record.setExpiresAt(now.plusSeconds(600));
    record.setPlanId(null);
    record.setAttempt(record.getAttempt() + 1);
    generations.save(record);
    return new Ticket(record.getId(), null, record.getAttempt());
  }

  @Transactional
  public void complete(Long userId, Ticket ticket, Long planId) {
    lock(userId);
    var record = generations.findById(ticket.generationId()).orElseThrow();
    if (record.getAttempt() != ticket.attempt()
        || !record.getUserId().equals(userId)
        || !"PENDING".equals(record.getStatus())
        || record.getExpiresAt().isBefore(Instant.now()))
      throw new BusinessException("생성 요청이 만료됐습니다. 다시 시도해 주세요.", HttpStatus.CONFLICT);
    var day = day(userId, record.getQuotaDate());
    day.setUsed(day.getUsed() + 1);
    record.setStatus("SUCCEEDED");
    record.setPlanId(planId);
  }

  @Transactional
  public void fail(Long userId, Ticket ticket) {
    lock(userId);
    generations
        .findById(ticket.generationId())
        .filter(
            r ->
                r.getAttempt() == ticket.attempt()
                    && r.getUserId().equals(userId)
                    && "PENDING".equals(r.getStatus()))
        .ifPresent(r -> r.setStatus("FAILED"));
  }

  @Transactional
  public QuotaStatusResponse getQuotaStatus(Long userId) {
    var d = getQuotaAdminDetail(userId);
    return new QuotaStatusResponse(
        d.effectivePlanPerDayLimit(), d.planPerDayUsed(), d.planPerDayRemaining(), 0, 0, 0);
  }

  @Transactional
  public QuotaAdminDetail getQuotaAdminDetail(Long userId) {
    lock(userId);
    LocalDate today = LocalDate.now(ZONE);
    var setting = settings.findByUserId(userId).orElse(null);
    Integer override = setting == null ? null : setting.getPlanPerDayLimit();
    int effective = override == null ? Math.max(1, planPerDayLimit) : override;
    int consumed = day(userId, today).getUsed();
    int reserved =
        (int)
            generations.countByUserIdAndQuotaDateAndStatusAndExpiresAtAfter(
                userId, today, "PENDING", Instant.now());
    return new QuotaAdminDetail(
        Math.max(1, planPerDayLimit),
        0,
        override,
        null,
        effective,
        0,
        consumed + reserved,
        Math.max(0, effective - consumed - reserved),
        0,
        0);
  }

  @Transactional
  public void upsertUserQuotaLimit(Long userId, Integer planLimit, Integer ignoredChatLimit) {
    lock(userId);
    if (planLimit == null || planLimit < 1 || planLimit > 1000)
      throw new BusinessException("일정 생성 한도는 1~1000 사이 정수여야 합니다.", HttpStatus.BAD_REQUEST);
    var setting =
        settings
            .findByUserId(userId)
            .orElseGet(() -> UserQuotaSetting.builder().userId(userId).build());
    setting.update(planLimit, null);
    settings.save(setting);
  }

  @Transactional
  public void resetQuotaToDefaultAndRefillToday(Long userId) {
    lock(userId);
    settings.deleteAllByUserId(userId);
    day(userId, LocalDate.now(ZONE)).setUsed(0);
  }

  private int limit(Long userId) {
    return settings
        .findByUserId(userId)
        .map(UserQuotaSetting::getPlanPerDayLimit)
        .orElse(Math.max(1, planPerDayLimit));
  }

  private void lock(Long userId) {
    users
        .lockById(userId)
        .orElseThrow(() -> new BusinessException("사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
  }

  private PlanUsageDay day(Long userId, LocalDate date) {
    return usage
        .findByUserIdAndQuotaDate(userId, date)
        .orElseGet(
            () -> {
              var d = new PlanUsageDay();
              d.setUserId(userId);
              d.setQuotaDate(date);
              var start =
                  date.atStartOfDay(ZONE).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
              var end =
                  date.plusDays(1)
                      .atStartOfDay(ZONE)
                      .withZoneSameInstant(ZoneOffset.UTC)
                      .toLocalDateTime();
              int baseline =
                  resets
                      .findByUserIdAndQuotaDate(userId, date)
                      .map(UserQuotaDailyReset::getPlanUsedBaseline)
                      .orElse(0);
              d.setUsed(
                  (int)
                      Math.max(
                          0,
                          plans.countByUserIdAndCreatedAtBetween(userId, start, end) - baseline));
              return usage.save(d);
            });
  }

  public record Ticket(Long generationId, Long existingPlanId, int attempt) {}

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
      int chatPerDayRemaining) {}
}
