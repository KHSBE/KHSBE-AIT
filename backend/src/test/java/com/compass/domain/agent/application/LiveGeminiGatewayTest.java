package com.compass.domain.agent.application;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import java.nio.file.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/** Opt-in provider smoke test. No Places key, application DB or user data is required. */
@EnabledIfEnvironmentVariable(named = "RUN_LIVE_AI_TEST", matches = "true")
class LiveGeminiGatewayTest {
  @Test
  void officialJavaHttpGatewayCanGenerateJson() throws Exception {
    var mapper = new ObjectMapper();
    GoogleCredentials credentials;
    try (var input = Files.newInputStream(Path.of("../secrets/gcp-sa.json"))) {
      credentials =
          GoogleCredentials.fromStream(input)
              .createScoped(List.of("https://www.googleapis.com/auth/cloud-platform"));
    }
    var gateway =
        new GeminiGateway(
            credentials, mapper, System.getenv("GOOGLE_PROJECT_ID"), "global", "gemini-4-argon");
    var schema =
        mapper.readTree(
            "{\"type\":\"OBJECT\",\"properties\":{\"answer\":{\"type\":\"STRING\"}},\"required\":[\"answer\"]}");
    var response = gateway.generate("answer 필드 값이 OK인 JSON만 출력하세요.", schema, false, 0, 0, 1024);
    assertEquals("OK", mapper.readTree(gateway.text(response)).path("answer").asText());
    System.out.println("LIVE_JAVA_GATEWAY_OK");
  }
}
