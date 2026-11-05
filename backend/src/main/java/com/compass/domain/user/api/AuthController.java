package com.compass.domain.user.api;

import com.compass.domain.user.application.AuthSessionService;
import com.compass.global.common.ApiResponse;
import com.compass.global.error.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
  private final AuthSessionService sessions;

  @Value("${app.cors.allowed-origins}")
  private String allowedOrigins;

  @Value("${app.auth.secure-cookies:true}")
  private boolean secure;

  @PostMapping("/exchange")
  public ApiResponse<Map<String, String>> exchange(
      @Valid @RequestBody CodeRequest code, HttpServletRequest req, HttpServletResponse res) {
    checkOrigin(req);
    return respond(sessions.exchange(code.code()), res);
  }

  @PostMapping("/refresh")
  public ApiResponse<Map<String, String>> refresh(
      @CookieValue(name = "compass_refresh", required = false) String cookie,
      HttpServletRequest req,
      HttpServletResponse res) {
    checkOrigin(req);
    return respond(sessions.refresh(cookie), res);
  }

  @PostMapping("/logout")
  public ApiResponse<Void> logout(
      @CookieValue(name = "compass_refresh", required = false) String cookie,
      HttpServletRequest req,
      HttpServletResponse res) {
    checkOrigin(req);
    sessions.logout(cookie);
    setCookie(res, "", Duration.ZERO);
    return ApiResponse.success(null);
  }

  private ApiResponse<Map<String, String>> respond(
      AuthSessionService.Session session, HttpServletResponse res) {
    setCookie(res, session.refreshToken(), Duration.between(Instant.now(), session.expiresAt()));
    res.setHeader("Cache-Control", "no-store");
    return ApiResponse.success(Map.of("accessToken", session.accessToken()));
  }

  private void setCookie(HttpServletResponse res, String value, Duration maxAge) {
    res.addHeader(
        "Set-Cookie",
        ResponseCookie.from("compass_refresh", value)
            .httpOnly(true)
            .secure(secure)
            .sameSite("Lax")
            .path("/api/auth")
            .maxAge(maxAge)
            .build()
            .toString());
  }

  private void checkOrigin(HttpServletRequest req) {
    String origin = req.getHeader("Origin");
    if (origin == null
        || Arrays.stream(allowedOrigins.split(",")).map(String::trim).noneMatch(origin::equals))
      throw new BusinessException("허용되지 않은 인증 요청입니다.", HttpStatus.FORBIDDEN);
  }

  public record CodeRequest(@NotBlank @Size(max = 200) String code) {}
}
