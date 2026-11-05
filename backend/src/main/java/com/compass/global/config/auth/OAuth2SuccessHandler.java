package com.compass.global.config.auth;

import com.compass.domain.user.dao.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

  private final com.compass.domain.user.application.AuthSessionService authSessions;
  private final UserRepository userRepository;

  @Value("${app.oauth2.redirect-uri:http://localhost:5173/oauth2/redirect}")
  private String frontendRedirectUri;

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication)
      throws IOException {

    var oAuth2User = (OAuth2User) authentication.getPrincipal();
    var email = (String) oAuth2User.getAttribute("email");

    var user =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> new IOException("OAuth user not found in local repository"));

    String code = authSessions.issueCode(user.getId());
    response.setHeader("Cache-Control", "no-store");
    response.setHeader("Referrer-Policy", "no-referrer");
    var targetUrl =
        UriComponentsBuilder.fromUriString(frontendRedirectUri)
            .queryParam("code", code)
            .build()
            .toUriString();
    getRedirectStrategy().sendRedirect(request, response, targetUrl);
  }
}
