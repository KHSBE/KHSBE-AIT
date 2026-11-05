package com.compass;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.google.auth.oauth2.GoogleCredentials;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:contexttest;DB_CLOSE_DELAY=-1",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "spring.jpa.show-sql=false",
      "spring.security.oauth2.client.registration.google.client-id=test-id",
      "spring.security.oauth2.client.registration.google.client-secret=test-secret",
      "spring.ai.google.genai.project-id=test-project",
      "spring.ai.google.genai.credentials-uri=classpath:not-used.json",
      "jwt.secret=test-secret-at-least-thirty-two-characters-long",
      "app.cors.allowed-origins=http://localhost:5173"
    })
@AutoConfigureMockMvc
class ApplicationContextTest {
  @MockitoBean GoogleCredentials credentials;
  @Autowired MockMvc mvc;

  @Test
  void anonymousGenerationIsUnauthorized() throws Exception {
    mvc.perform(post("/api/agent/plan").contentType("application/json").content("{}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void refreshFromUntrustedOriginIsForbidden() throws Exception {
    mvc.perform(post("/api/auth/refresh").header("Origin", "https://evil.example"))
        .andExpect(status().isForbidden());
  }

  @Test
  void backendRootAndHealthArePublicAndNotCached() throws Exception {
    for (String path : new String[] {"/", "/healthz"}) {
      mvc.perform(get(path))
          .andExpect(status().isOk())
          .andExpect(header().string("Cache-Control", "no-store"))
          .andExpect(jsonPath("$.data.service").value("compass-backend"))
          .andExpect(jsonPath("$.data.status").value("UP"));
    }
  }

  @Test
  void missingPermittedResourceReturns404InsteadOf500() throws Exception {
    mvc.perform(get("/login/does-not-exist"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.success").value(false));
  }

  @Test
  void refreshWithoutCookieHasTraceAndReturns401InsteadOfProxyError() throws Exception {
    mvc.perform(post("/api/auth/refresh").header("Origin", "http://localhost:5173"))
        .andExpect(status().isUnauthorized())
        .andExpect(header().exists("X-Compass-Request-Id"));
  }
}
