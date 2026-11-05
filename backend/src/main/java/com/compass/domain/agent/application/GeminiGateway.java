package com.compass.domain.agent.application;

import com.compass.global.error.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
@Slf4j
public class GeminiGateway {
  private final GoogleCredentials credentials;
  private final ObjectMapper mapper;
  private final RestClient client;
  private final String project;
  private final String location;
  private final String model;

  public GeminiGateway(
      GoogleCredentials credentials,
      ObjectMapper mapper,
      @Value("${spring.ai.google.genai.project-id}") String project,
      @Value("${spring.ai.google.genai.location:global}") String location,
      @Value("${app.ai.model.plan:gemini-4-argon}") String model) {
    this.credentials = credentials;
    this.mapper = mapper;
    this.project = project;
    this.location = location;
    this.model = model;
    var factory =
        new JdkClientHttpRequestFactory(
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
    factory.setReadTimeout(Duration.ofSeconds(100));
    String host =
        location.equals("global")
            ? "aiplatform.googleapis.com"
            : location + "-aiplatform.googleapis.com";
    this.client = RestClient.builder().baseUrl("https://" + host).requestFactory(factory).build();
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
      // Explicit Places-only grounding prevents unrequested routing queries.
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
    long started = System.nanoTime();
    try {
      credentials.refreshIfExpired();
      JsonNode response =
          client
              .post()
              .uri(
                  "/v1beta1/projects/{project}/locations/{location}/publishers/google/models/{model}:generateContent",
                  project,
                  location,
                  model)
              .header("Authorization", "Bearer " + credentials.getAccessToken().getTokenValue())
              .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
              .body(body)
              .retrieve()
              .body(JsonNode.class);
      if (response == null
          || !response.path("candidates").isArray()
          || response.path("candidates").isEmpty()) {
        throw new BusinessException("AI 응답이 비어 있습니다. 다시 시도해 주세요.", HttpStatus.BAD_GATEWAY);
      }
      String finish = response.path("candidates").get(0).path("finishReason").asText();
      if (!"STOP".equals(finish)) {
        throw new BusinessException(
            "AI가 일정을 완성하지 못했습니다. 조건을 줄여 다시 시도해 주세요.", HttpStatus.BAD_GATEWAY);
      }
      log.info(
          "event=ai_complete model={} maps={} elapsedMs={} totalTokens={}",
          model,
          maps,
          (System.nanoTime() - started) / 1_000_000,
          response.path("usageMetadata").path("totalTokenCount").asInt());
      return response;
    } catch (RestClientResponseException e) {
      log.warn("event=ai_failed model={} http={}", model, e.getStatusCode().value());
      throw new BusinessException(
          e.getStatusCode().value() == 429
              ? "AI 제공자의 요청 한도에 도달했습니다. 잠시 후 다시 시도해 주세요."
              : "AI 연결에 실패했습니다. 잠시 후 다시 시도해 주세요.",
          HttpStatus.BAD_GATEWAY);
    } catch (BusinessException e) {
      throw e;
    } catch (Exception e) {
      log.warn("event=ai_failed model={} type={}", model, e.getClass().getSimpleName());
      throw new BusinessException("AI 연결 시간이 초과되었거나 인증에 실패했습니다.", HttpStatus.BAD_GATEWAY);
    }
  }

  public String text(JsonNode response) {
    StringBuilder text = new StringBuilder();
    for (JsonNode part : response.path("candidates").get(0).path("content").path("parts")) {
      if (!part.path("thought").asBoolean(false)) text.append(part.path("text").asText(""));
    }
    return text.toString();
  }
}
