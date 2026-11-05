package com.compass.domain.agent.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import com.compass.global.error.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.*;
import java.io.IOException;
import java.net.http.HttpTimeoutException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.*;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@ExtendWith(OutputCaptureExtension.class)
class GeminiGatewayTest {
  static final String URL =
      "https://aiplatform.googleapis.com/v1beta1/projects/test-project/locations/global/publishers/google/models/gemini-4-argon:generateContent";
  static final String SUCCESS =
      "{\"candidates\":[{\"finishReason\":\"STOP\",\"content\":{\"parts\":[{\"text\":\"OK\"}]}}]}";
  static final String BUSY =
      "{\"error\":{\"code\":429,\"status\":\"RESOURCE_EXHAUSTED\",\"message\":\"Resource exhausted,"
          + " please try again later.\"}}";
  final ObjectMapper mapper = new ObjectMapper();
  final GoogleCredentials credentials = mock(GoogleCredentials.class);
  final AtomicLong clock = new AtomicLong();
  final List<Long> delays = new ArrayList<>();
  MockRestServiceServer server;
  GeminiGateway gateway;

  @BeforeEach
  void setup() {
    when(credentials.getAccessToken())
        .thenReturn(
            new AccessToken(
                "not-for-logging-token", new Date(System.currentTimeMillis() + 3600000)));
    var builder = RestClient.builder().baseUrl("https://aiplatform.googleapis.com");
    server = MockRestServiceServer.bindTo(builder).build();
    var client = builder.build();
    gateway =
        new GeminiGateway(
            credentials,
            mapper,
            "test-project",
            "global",
            "gemini-4-argon",
            client,
            client,
            millis -> {
              delays.add(millis);
              clock.addAndGet(millis * 1_000_000);
            },
            clock::get);
  }

  void call(boolean maps) {
    gateway.generate("sensitive-user-notes", null, maps, 35, 135, 256);
  }

  @Test
  void transient429RetriesAndSucceeds(CapturedOutput output) {
    server
        .expect(requestTo(URL))
        .andRespond(
            withStatus(HttpStatus.TOO_MANY_REQUESTS)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BUSY));
    server.expect(requestTo(URL)).andRespond(withSuccess(SUCCESS, MediaType.APPLICATION_JSON));
    call(true);
    server.verify();
    assertEquals(1, delays.size());
    assertTrue(output.getAll().contains("stage=maps_grounding"));
    assertFalse(output.getAll().contains("sensitive-user-notes"));
    assertFalse(output.getAll().contains("not-for-logging-token"));
  }

  @Test
  void retryCountIsBounded() {
    for (int n = 0; n < 3; n++)
      server
          .expect(requestTo(URL))
          .andRespond(
              withStatus(HttpStatus.TOO_MANY_REQUESTS)
                  .contentType(MediaType.APPLICATION_JSON)
                  .body(BUSY));
    var e = assertThrows(AiUnavailableException.class, () -> call(false));
    assertEquals(HttpStatus.SERVICE_UNAVAILABLE, e.getHttpStatus());
    assertEquals(2, delays.size());
    server.verify();
  }

  @Test
  void explicitQuotaFailureDoesNotRetryOrMasqueradeAsCapacity(CapturedOutput output) {
    String body =
        "{\"error\":{\"status\":\"RESOURCE_EXHAUSTED\",\"message\":\"Quota exceeded: private"
            + " provider"
            + " message\",\"details\":[{\"@type\":\"type.googleapis.com/google.rpc.QuotaFailure\",\"violations\":[{}]},{\"@type\":\"type.googleapis.com/google.rpc.ErrorInfo\",\"reason\":\"QUOTA_EXCEEDED\",\"metadata\":{\"quota_metric\":\"aiplatform.googleapis.com/test_requests\"}}]}}";
    server
        .expect(requestTo(URL))
        .andRespond(
            withStatus(HttpStatus.TOO_MANY_REQUESTS)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body));
    var e = assertThrows(BusinessException.class, () -> call(true));
    assertFalse(e instanceof AiUnavailableException);
    assertTrue(e.getMessage().contains("사용량 제한"));
    assertTrue(delays.isEmpty());
    server.verify();
    assertTrue(output.getAll().contains("quota=true"));
    assertTrue(output.getAll().contains("reason=QUOTA_EXCEEDED"));
    assertFalse(output.getAll().contains("private provider message"));
  }

  @Test
  void serviceUnavailableRetries() {
    server
        .expect(requestTo(URL))
        .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE).body("upstream unavailable"));
    server.expect(requestTo(URL)).andRespond(withSuccess(SUCCESS, MediaType.APPLICATION_JSON));
    call(false);
    assertEquals(1, delays.size());
    server.verify();
  }

  @Test
  void longRetryAfterIsNotIgnored() {
    var headers = new HttpHeaders();
    headers.set(HttpHeaders.RETRY_AFTER, "60");
    server
        .expect(requestTo(URL))
        .andRespond(
            withStatus(HttpStatus.TOO_MANY_REQUESTS)
                .headers(headers)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BUSY));
    assertThrows(AiUnavailableException.class, () -> call(false));
    assertTrue(delays.isEmpty());
    server.verify();
  }

  @Test
  void fractionalRetryInfoIsRespected() {
    String body =
        "{\"error\":{\"status\":\"RESOURCE_EXHAUSTED\",\"details\":[{\"@type\":\"type.googleapis.com/google.rpc.RetryInfo\",\"retryDelay\":\"4.5s\"}]}}";
    server
        .expect(requestTo(URL))
        .andRespond(
            withStatus(HttpStatus.TOO_MANY_REQUESTS)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body));
    server.expect(requestTo(URL)).andRespond(withSuccess(SUCCESS, MediaType.APPLICATION_JSON));
    call(false);
    assertEquals(4500L, delays.get(0));
    server.verify();
  }

  @Test
  void authenticationErrorIsNotRetriedOrReportedAsTimeout() {
    server
        .expect(requestTo(URL))
        .andRespond(
            withStatus(HttpStatus.FORBIDDEN)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\":{\"status\":\"PERMISSION_DENIED\"}}"));
    var e = assertThrows(BusinessException.class, () -> call(false));
    assertFalse(e instanceof AiUnavailableException);
    assertTrue(e.getMessage().contains("접근 권한"));
    assertTrue(delays.isEmpty());
    server.verify();
  }

  @Test
  void httpTimeoutDoesNotDuplicatePostOrClaimAuthenticationFailure(CapturedOutput output) {
    server
        .expect(requestTo(URL))
        .andRespond(withException(new HttpTimeoutException("request timed out")));
    var e = assertThrows(AiUnavailableException.class, () -> call(false));
    assertEquals(HttpStatus.GATEWAY_TIMEOUT, e.getHttpStatus());
    assertTrue(e.getMessage().contains("대기 시간"));
    assertFalse(e.getMessage().contains("인증"));
    assertTrue(delays.isEmpty());
    server.verify();
    assertTrue(output.getAll().contains("category=TIMEOUT"));
  }

  @Test
  void networkFailureIsDistinguishedFromAuthentication() {
    server
        .expect(requestTo(URL))
        .andRespond(withException(new java.net.ConnectException("network unavailable")));
    var e = assertThrows(AiUnavailableException.class, () -> call(false));
    assertTrue(e.getMessage().contains("연결하지 못"));
    assertFalse(e.getMessage().contains("인증"));
    assertTrue(delays.isEmpty());
    server.verify();
  }

  @Test
  void tokenRefreshFailureIsReportedBeforeGenerationRequest() throws Exception {
    doThrow(new IOException("credential failure")).when(credentials).refreshIfExpired();
    var e = assertThrows(BusinessException.class, () -> call(false));
    assertFalse(e instanceof AiUnavailableException);
    assertTrue(e.getMessage().contains("서비스 계정 인증"));
    assertTrue(delays.isEmpty());
    server.verify();
  }

  @Test
  void invalidResponseIsNotReportedAsAuthenticationFailure() {
    server.expect(requestTo(URL)).andRespond(withSuccess("not-json", MediaType.APPLICATION_JSON));
    var e = assertThrows(BusinessException.class, () -> call(false));
    assertTrue(e.getMessage().contains("응답을 읽지"));
    assertTrue(delays.isEmpty());
    server.verify();
  }

  @Test
  void slowFailureDoesNotStartAnotherFullHttpWindow() {
    server
        .expect(requestTo(URL))
        .andRespond(
            request -> {
              clock.set(20_000_000_000L);
              return withStatus(HttpStatus.TOO_MANY_REQUESTS)
                  .contentType(MediaType.APPLICATION_JSON)
                  .body(BUSY)
                  .createResponse(request);
            });
    assertThrows(AiUnavailableException.class, () -> call(false));
    assertTrue(delays.isEmpty());
    server.verify();
  }

  @Test
  void explicitQuotaFailureOn503AlsoDoesNotRetryOrFallback() {
    String body =
        "{\"error\":{\"status\":\"RESOURCE_EXHAUSTED\",\"details\":[{\"@type\":\"type.googleapis.com/google.rpc.QuotaFailure\"}]}}";
    server
        .expect(requestTo(URL))
        .andRespond(
            withStatus(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body));
    var failure = assertThrows(BusinessException.class, () -> call(true));
    assertFalse(failure instanceof AiUnavailableException);
    assertTrue(delays.isEmpty());
    server.verify();
  }
}
