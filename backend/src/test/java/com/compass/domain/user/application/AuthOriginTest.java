package com.compass.domain.user.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.compass.domain.user.api.AuthController;
import com.compass.global.error.BusinessException;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.*;
import org.springframework.test.util.ReflectionTestUtils;

class AuthOriginTest {
  @Test
  void untrustedOriginCannotUseRefreshCookie() {
    var service = mock(AuthSessionService.class);
    var controller = new AuthController(service);
    ReflectionTestUtils.setField(controller, "allowedOrigins", "https://www.compass-travel.site");
    var request = new MockHttpServletRequest();
    request.addHeader("Origin", "https://evil.example");
    var error =
        assertThrows(
            BusinessException.class,
            () -> controller.refresh("token", request, new MockHttpServletResponse()));
    assertEquals(HttpStatus.FORBIDDEN, error.getHttpStatus());
    verifyNoInteractions(service);
  }

  @Test
  void allowedOriginReceivesHttpOnlySecureCookieNotRefreshInBody() {
    var service = mock(AuthSessionService.class);
    var controller = new AuthController(service);
    ReflectionTestUtils.setField(controller, "allowedOrigins", "https://www.compass-travel.site");
    ReflectionTestUtils.setField(controller, "secure", true);
    when(service.refresh("old"))
        .thenReturn(
            new AuthSessionService.Session(
                "access", "new-refresh", Instant.now().plusSeconds(300)));
    var request = new MockHttpServletRequest();
    request.addHeader("Origin", "https://www.compass-travel.site");
    var response = new MockHttpServletResponse();
    var body = controller.refresh("old", request, response);
    var cookie = response.getHeader("Set-Cookie");
    assertNotNull(cookie);
    assertTrue(cookie.contains("HttpOnly"));
    assertTrue(cookie.contains("Secure"));
    assertTrue(cookie.contains("SameSite=Lax"));
    assertFalse(body.toString().contains("new-refresh"));
  }
}
