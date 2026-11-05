package com.compass.domain.agent.application;

import com.compass.global.error.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.http.HttpClient;
import java.net.http.HttpTimeoutException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;

@Component
@Slf4j
public class GeminiGateway {
  private static final int MAX_ATTEMPTS = 3;
  private static final long MAPS_TIMEOUT_MS = 45_000;
  private static final long PLAN_TIMEOUT_MS = 100_000;
  private static final long RETRY_ALLOWANCE_MS = 15_000;
  private final GoogleCredentials credentials;
  private final ObjectMapper mapper;
  private final RestClient mapsClient;
  private final RestClient planClient;
  private final String project;
  private final String location;
  private final String model;
  private final LongConsumer sleeper;
  private final LongSupplier nanoTime;

  @Autowired
  public GeminiGateway(
      GoogleCredentials credentials,
      ObjectMapper mapper,
      @Value("${spring.ai.google.genai.project-id}") String project,
      @Value("${spring.ai.google.genai.location:global}") String location,
      @Value("${app.ai.model.plan:gemini-4-argon}") String model) {
    this(
        credentials,
        mapper,
        project,
        location,
        model,
        client(location, MAPS_TIMEOUT_MS),
        client(location, PLAN_TIMEOUT_MS),
        GeminiGateway::pause,
        System::nanoTime);
  }

  GeminiGateway(
      GoogleCredentials credentials,
      ObjectMapper mapper,
      String project,
      String location,
      String model,
      RestClient mapsClient,
      RestClient planClient,
      LongConsumer sleeper,
      LongSupplier nanoTime) {
    this.credentials = credentials;
    this.mapper = mapper;
    this.project = project;
    this.location = location;
    this.model = model;
    this.mapsClient = mapsClient;
    this.planClient = planClient;
    this.sleeper = sleeper;
    this.nanoTime = nanoTime;
  }

  private static RestClient client(String location, long timeoutMs) {
    var factory =
        new JdkClientHttpRequestFactory(
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
    factory.setReadTimeout(Duration.ofMillis(timeoutMs));
    String host =
        location.equals("global")
            ? "aiplatform.googleapis.com"
            : location + "-aiplatform.googleapis.com";
    return RestClient.builder().baseUrl("https://" + host).requestFactory(factory).build();
  }

  public JsonNode generate(
      String prompt, JsonNode schema, boolean maps, double lat, double lng, int limit) {
    Map<String, Object> config = new LinkedHashMap<>();
    config.put("maxOutputTokens", limit);
    config.put("thinkingConfig", Map.of("thinkingLevel", maps ? "LOW" : "MEDIUM"));
    if (schema != null) {
      config.put("responseMimeType", "application/json");
      config.put("responseSchema", schema);
    }
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", prompt)))));
    body.put("generationConfig", config);
    if (maps) {
      body.put(
          "tools",
          List.of(Map.of("googleMaps", Map.of("groundingTypes", Map.of("places", Map.of())))));
      body.put(
          "toolConfig",
          Map.of(
              "retrievalConfig",
              Map.of(
                  "latLng", Map.of("latitude", lat, "longitude", lng), "languageCode", "ko_KR")));
    }
    String stage = maps ? "maps_grounding" : "schedule";
    String trace = UUID.randomUUID().toString();
    long started = nanoTime.getAsLong();
    long timeout = maps ? MAPS_TIMEOUT_MS : PLAN_TIMEOUT_MS;
    log.info(
        "event=ai_start trace={} model={} stage={} timeoutMs={}", trace, model, stage, timeout);
    for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
      String token = accessToken(stage, trace);
      try {
        JsonNode response =
            (maps ? mapsClient : planClient)
                .post()
                .uri(
                    "/v1beta1/projects/{project}/locations/{location}/publishers/google/models/{model}:generateContent",
                    project,
                    location,
                    model)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);
        requireComplete(response, stage, trace);
        log.info(
            "event=ai_complete trace={} model={} stage={} attempt={} elapsedMs={} totalTokens={}",
            trace,
            model,
            stage,
            attempt,
            elapsed(started),
            response.path("usageMetadata").path("totalTokenCount").asInt());
        return response;
      } catch (RestClientResponseException error) {
        int http = error.getStatusCode().value();
        var diagnostic = diagnose(error);
        long delay = retryDelay(error, attempt);
        boolean retryable = !diagnostic.quota() && (http == 429 || http == 503);
        // Reserve enough time for one full HTTP attempt; never stack unbounded retries.
        boolean retry =
            retryable
                && attempt < MAX_ATTEMPTS
                && delay <= 8_000
                && elapsed(started) + delay <= RETRY_ALLOWANCE_MS;
        log.warn(
            "event=ai_failed trace={} model={} stage={} attempt={} http={} providerStatus={}"
                + " reason={} quotaMetric={} quota={} retry={} elapsedMs={}",
            trace,
            model,
            stage,
            attempt,
            http,
            diagnostic.status(),
            diagnostic.reason(),
            diagnostic.quotaMetric(),
            diagnostic.quota(),
            retry,
            elapsed(started));
        if (retry) {
          sleeper.accept(delay);
          continue;
        }
        if (!diagnostic.quota() && (http == 429 || http == 503))
          throw new AiUnavailableException(
              "AI 서버의 일시적인 처리 용량 또는 제공자 제한으로 응답을 받지 못했습니다. 잠시 후 다시 시도해 주세요.",
              HttpStatus.SERVICE_UNAVAILABLE);
        if (diagnostic.quota() && (http == 429 || http == 503))
          throw new BusinessException(
              "AI 제공자의 사용량 제한으로 일정을 만들지 못했습니다. 잠시 후 다시 시도해 주세요.", HttpStatus.BAD_GATEWAY);
        if (http == 401 || http == 403)
          throw new BusinessException("AI 인증 또는 접근 권한 설정을 확인해야 합니다.", HttpStatus.BAD_GATEWAY);
        throw new BusinessException(
            "AI 제공자가 요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.", HttpStatus.BAD_GATEWAY);
      } catch (ResourceAccessException error) {
        boolean timeoutFailure = timeout(error);
        log.warn(
            "event=ai_failed trace={} model={} stage={} attempt={} category={} rootType={}"
                + " elapsedMs={}",
            trace,
            model,
            stage,
            attempt,
            timeoutFailure ? "TIMEOUT" : "NETWORK",
            root(error).getClass().getSimpleName(),
            elapsed(started));
        // A timed-out POST may have been processed; do not automatically send it again.
        throw new AiUnavailableException(
            timeoutFailure
                ? "AI 응답 대기 시간이 초과되었습니다. 잠시 후 다시 시도해 주세요."
                : "AI 서버에 연결하지 못했습니다. 잠시 후 다시 시도해 주세요.",
            timeoutFailure ? HttpStatus.GATEWAY_TIMEOUT : HttpStatus.BAD_GATEWAY);
      } catch (BusinessException error) {
        throw error;
      } catch (RuntimeException error) {
        log.warn(
            "event=ai_failed trace={} model={} stage={} category=INVALID_RESPONSE rootType={}"
                + " elapsedMs={}",
            trace,
            model,
            stage,
            root(error).getClass().getSimpleName(),
            elapsed(started));
        throw new BusinessException("AI 응답을 읽지 못했습니다. 잠시 후 다시 시도해 주세요.", HttpStatus.BAD_GATEWAY);
      }
    }
    throw new IllegalStateException("Unreachable retry state");
  }

  private String accessToken(String stage, String trace) {
    try {
      credentials.refreshIfExpired();
      var token = credentials.getAccessToken();
      if (token == null) throw new IllegalStateException("No access token");
      return token.getTokenValue();
    } catch (IOException | RuntimeException error) {
      boolean timedOut = timeout(error);
      log.warn(
          "event=ai_failed trace={} model={} stage={} category=AUTH_REFRESH rootType={}",
          trace,
          model,
          stage,
          root(error).getClass().getSimpleName());
      throw new BusinessException(
          timedOut ? "AI 인증 서버의 응답 시간이 초과되었습니다." : "AI 서비스 계정 인증에 실패했습니다. 서버 인증 설정을 확인해야 합니다.",
          timedOut ? HttpStatus.GATEWAY_TIMEOUT : HttpStatus.BAD_GATEWAY);
    }
  }

  private void requireComplete(JsonNode response, String stage, String trace) {
    if (response == null
        || !response.path("candidates").isArray()
        || response.path("candidates").isEmpty()) {
      log.warn(
          "event=ai_failed trace={} model={} stage={} category=EMPTY_RESPONSE",
          trace,
          model,
          stage);
      throw new BusinessException("AI 응답이 비어 있습니다. 다시 시도해 주세요.", HttpStatus.BAD_GATEWAY);
    }
    String finish = response.path("candidates").get(0).path("finishReason").asText();
    if (!"STOP".equals(finish)) {
      log.warn(
          "event=ai_failed trace={} model={} stage={} category=INCOMPLETE_RESPONSE finish={}",
          trace,
          model,
          stage,
          safe(finish));
      throw new BusinessException("AI가 일정을 완성하지 못했습니다. 조건을 줄여 다시 시도해 주세요.", HttpStatus.BAD_GATEWAY);
    }
  }

  private Diagnostic diagnose(RestClientResponseException error) {
    String status = "UNKNOWN", reason = "UNKNOWN", metric = "NONE";
    boolean quota = false;
    try {
      var node = mapper.readTree(error.getResponseBodyAsString()).path("error");
      status = safe(node.path("status").asText());
      String message = node.path("message").asText().toLowerCase(Locale.ROOT);
      quota =
          message.contains("quota exceeded")
              || message.contains("quota limit")
              || message.contains("daily limit");
      for (var detail : node.path("details")) {
        if (detail.path("@type").asText().endsWith("QuotaFailure")) quota = true;
        if (detail.has("reason")) reason = safe(detail.path("reason").asText());
        if (detail.path("metadata").has("quota_metric"))
          metric = safe(detail.path("metadata").path("quota_metric").asText());
      }
      quota |= reason.equals("QUOTA_EXCEEDED") || reason.equals("RATE_LIMIT_EXCEEDED");
    } catch (Exception ignored) {
      /* A gateway may return a non-JSON error page. */
    }
    return new Diagnostic(status, reason, metric, quota);
  }

  private long retryDelay(RestClientResponseException error, int attempt) {
    long delay = (1L << attempt) * 1_000 + ThreadLocalRandom.current().nextLong(250);
    String header =
        error.getResponseHeaders() == null
            ? null
            : error.getResponseHeaders().getFirst(HttpHeaders.RETRY_AFTER);
    if (header != null) {
      try {
        delay = Math.max(delay, Math.multiplyExact(Long.parseLong(header), 1_000));
      } catch (Exception ignored) {
        try {
          delay =
              Math.max(
                  delay,
                  Duration.between(
                          Instant.now(),
                          ZonedDateTime.parse(header, DateTimeFormatter.RFC_1123_DATE_TIME)
                              .toInstant())
                      .toMillis());
        } catch (Exception ignoredDate) {
        }
      }
    }
    try {
      for (var detail :
          mapper.readTree(error.getResponseBodyAsString()).path("error").path("details")) {
        if (detail.path("@type").asText().endsWith("RetryInfo")) {
          String seconds = detail.path("retryDelay").asText();
          if (seconds.matches("[0-9]+(?:\\.[0-9]+)?s"))
            delay =
                Math.max(
                    delay,
                    (long)
                        Math.ceil(
                            Double.parseDouble(seconds.substring(0, seconds.length() - 1))
                                * 1_000));
        }
      }
    } catch (Exception ignored) {
    }
    return delay;
  }

  private long elapsed(long started) {
    return Math.max(0, (nanoTime.getAsLong() - started) / 1_000_000);
  }

  private static boolean timeout(Throwable error) {
    for (int n = 0; error != null && n < 20; n++, error = error.getCause())
      if (error instanceof HttpTimeoutException || error instanceof SocketTimeoutException)
        return true;
    return false;
  }

  private static Throwable root(Throwable error) {
    for (int n = 0; error.getCause() != null && error.getCause() != error && n < 20; n++)
      error = error.getCause();
    return error;
  }

  private static String safe(String value) {
    return value != null && value.matches("[A-Za-z0-9_./:-]{1,180}") ? value : "UNKNOWN";
  }

  private static void pause(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException error) {
      Thread.currentThread().interrupt();
      throw new BusinessException("AI 요청 처리가 중단되었습니다. 다시 시도해 주세요.", HttpStatus.SERVICE_UNAVAILABLE);
    }
  }

  private record Diagnostic(String status, String reason, String quotaMetric, boolean quota) {}

  public String text(JsonNode response) {
    StringBuilder text = new StringBuilder();
    for (JsonNode part : response.path("candidates").get(0).path("content").path("parts"))
      if (!part.path("thought").asBoolean(false)) text.append(part.path("text").asText(""));
    return text.toString();
  }
}
