package com.compass.domain.user.application;

import com.compass.domain.user.dao.*;
import com.compass.domain.user.domain.AuthGrant;
import com.compass.global.config.auth.JwtTokenProvider;
import com.compass.global.error.BusinessException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthSessionService {
  private final AuthGrantRepository grants;
  private final UserRepository users;
  private final JwtTokenProvider tokens;
  private static final SecureRandom RANDOM = new SecureRandom();

  @Transactional
  public String issueCode(Long userId) {
    return issue(userId, UUID.randomUUID().toString(), "CODE", Instant.now().plusSeconds(90));
  }

  @Transactional
  public Session exchange(String code) {
    AuthGrant grant = consume(code, "CODE");
    return session(grant.getUserId(), grant.getFamilyId(), Instant.now().plusSeconds(14 * 86400));
  }

  @Transactional
  public Session refresh(String token) {
    AuthGrant grant = consume(token, "REFRESH");
    return session(grant.getUserId(), grant.getFamilyId(), grant.getExpiresAt());
  }

  @Transactional
  public void logout(String token) {
    if (token == null) return;
    grants.lockByHash(hash(token)).ifPresent(g -> grants.revokeFamily(g.getFamilyId()));
  }

  private AuthGrant consume(String raw, String kind) {
    if (raw == null || raw.length() > 200) throw unauthorized();
    var grant = grants.lockByHash(hash(raw)).orElseThrow(this::unauthorized);
    if (grant.isUsed()
        || !kind.equals(grant.getKind())
        || !grant.getExpiresAt().isAfter(Instant.now())) throw unauthorized();
    grant.setUsed(true);
    return grant;
  }

  private Session session(Long userId, String family, Instant expiry) {
    var user = users.findById(userId).orElseThrow(this::unauthorized);
    String refresh = issue(userId, family, "REFRESH", expiry);
    return new Session(
        tokens.createToken(user.getEmail(), user.getRoleOrDefault().name()), refresh, expiry);
  }

  private String issue(Long userId, String family, String kind, Instant expires) {
    byte[] bytes = new byte[32];
    RANDOM.nextBytes(bytes);
    String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    var grant = new AuthGrant();
    grant.setTokenHash(hash(raw));
    grant.setUserId(userId);
    grant.setFamilyId(family);
    grant.setKind(kind);
    grant.setExpiresAt(expires);
    grants.save(grant);
    return raw;
  }

  private String hash(String raw) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private BusinessException unauthorized() {
    return new BusinessException("로그인이 만료되었습니다. 다시 로그인해 주세요.", HttpStatus.UNAUTHORIZED);
  }

  public record Session(String accessToken, String refreshToken, Instant expiresAt) {}
}
