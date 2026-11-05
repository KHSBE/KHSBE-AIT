package com.compass.global.config.auth;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(OutputCaptureExtension.class)
class AuthRequestLoggingFilterTest {
  @Test
  void tracesAuthRequestWithoutLoggingCredentials(CapturedOutput output) throws Exception {
    var request = new MockHttpServletRequest("POST", "/api/auth/refresh");
    request.setQueryString("code=private-query-secret");
    request.addHeader("Authorization", "Bearer private-bearer-secret");
    request.addHeader("Cookie", "compass_refresh=private-cookie-secret");
    request.setContent("private-body-secret".getBytes(StandardCharsets.UTF_8));
    var response = new MockHttpServletResponse();
    new AuthRequestLoggingFilter()
        .doFilter(request, response, (req, res) -> response.setStatus(401));
    assertNotNull(response.getHeader("X-Compass-Request-Id"));
    assertTrue(output.getAll().contains("event=auth_start"));
    assertTrue(output.getAll().contains("status=401"));
    assertFalse(output.getAll().contains("private-query-secret"));
    assertFalse(output.getAll().contains("private-bearer-secret"));
    assertFalse(output.getAll().contains("private-cookie-secret"));
    assertFalse(output.getAll().contains("private-body-secret"));
  }

  @Test
  void propagatedExceptionIsNotLoggedAsSuccess(CapturedOutput output) {
    var request = new MockHttpServletRequest("POST", "/api/auth/refresh");
    var response = new MockHttpServletResponse();
    var failure = new IOException("private-exception-secret");
    assertSame(
        failure,
        assertThrows(
            IOException.class,
            () ->
                new AuthRequestLoggingFilter()
                    .doFilter(
                        request,
                        response,
                        (req, res) -> {
                          throw failure;
                        })));
    assertTrue(output.getAll().contains("status=500"));
    assertFalse(output.getAll().contains("private-exception-secret"));
  }

  @Test
  void unrelatedRequestsAreNotTraced(CapturedOutput output) throws Exception {
    var response = new MockHttpServletResponse();
    new AuthRequestLoggingFilter()
        .doFilter(
            new MockHttpServletRequest("GET", "/healthz"),
            response,
            (req, res) -> response.setStatus(200));
    assertNull(response.getHeader("X-Compass-Request-Id"));
    assertFalse(output.getAll().contains("event=auth_start"));
  }
}
