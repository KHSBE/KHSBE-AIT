package com.compass.global.config.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@Slf4j
public class AuthRequestLoggingFilter extends OncePerRequestFilter {
  private static final Set<String> PATHS =
      Set.of("/api/auth/exchange", "/api/auth/refresh", "/api/auth/logout");

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !PATHS.contains(request.getRequestURI());
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String trace = UUID.randomUUID().toString();
    String path = request.getRequestURI();
    long started = System.nanoTime();
    int failedStatus = 0;
    response.setHeader("X-Compass-Request-Id", trace);
    log.info("event=auth_start trace={} method={} path={}", trace, request.getMethod(), path);
    try {
      chain.doFilter(request, response);
    } catch (ServletException | IOException | RuntimeException error) {
      failedStatus = 500;
      throw error;
    } finally {
      // Do not log query strings, bodies, credentials, cookies, or exception messages.
      log.info(
          "event=auth_end trace={} method={} path={} status={} elapsedMs={}",
          trace,
          request.getMethod(),
          path,
          failedStatus == 0 ? response.getStatus() : failedStatus,
          (System.nanoTime() - started) / 1_000_000);
    }
  }
}
