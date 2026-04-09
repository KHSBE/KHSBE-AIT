package com.compass.global.config.auth;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Component
public class JwtTokenProvider {

    private static final String CLAIM_TOKEN_TYPE = "tokenType";
    private static final String CLAIM_ROLE = "role";

    private static final String TOKEN_TYPE_ACCESS = "ACCESS";
    private static final String TOKEN_TYPE_REFRESH = "REFRESH";

    private static final String DEFAULT_ROLE = "USER";

    @Value("${jwt.secret}")
    private String secretKey;

    private final long tokenValidTime = 30 * 60 * 1000L;
    private final long refreshTokenValidTime = 14 * 24 * 60 * 60 * 1000L;

    private Key key;

    @PostConstruct
    protected void init() {
        var encodedKey = Base64.getEncoder().encodeToString(secretKey.getBytes());
        this.key = Keys.hmacShaKeyFor(encodedKey.getBytes());
    }

    public String createToken(String email) {
        return createToken(email, DEFAULT_ROLE);
    }

    public String createToken(String email, String role) {
        return createJwt(email, role, tokenValidTime, TOKEN_TYPE_ACCESS);
    }

    public String createRefreshToken(String email) {
        return createRefreshToken(email, DEFAULT_ROLE);
    }

    public String createRefreshToken(String email, String role) {
        return createJwt(email, role, refreshTokenValidTime, TOKEN_TYPE_REFRESH);
    }

    private String createJwt(String email, String role, long validTime, String tokenType) {
        var claims = Jwts.claims().setSubject(email);
        claims.putAll(Map.of(
                CLAIM_TOKEN_TYPE, tokenType,
                CLAIM_ROLE, normalizeRole(role)
        ));

        var now = new Date();
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + validTime))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Authentication getAuthentication(String token) {
        if (!validateAccessToken(token)) {
            throw new JwtException("Invalid access token type");
        }

        String email = getUserEmail(token);
        String role = getUserRole(token);

        return new UsernamePasswordAuthenticationToken(
                email,
                "",
                List.of(new SimpleGrantedAuthority("ROLE_" + normalizeRole(role)))
        );
    }

    public String getUserEmail(String token) {
        return parseClaims(token).getBody().getSubject();
    }

    public String getUserRole(String token) {
        try {
            var claims = parseClaims(token).getBody();
            Object role = claims.get(CLAIM_ROLE);
            return normalizeRole(role == null ? DEFAULT_ROLE : String.valueOf(role));
        } catch (Exception e) {
            return DEFAULT_ROLE;
        }
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean validateAccessToken(String token) {
        return validateTokenType(token, TOKEN_TYPE_ACCESS);
    }

    public boolean validateRefreshToken(String token) {
        return validateTokenType(token, TOKEN_TYPE_REFRESH);
    }

    private String getTokenType(String token) {
        try {
            var claims = parseClaims(token).getBody();
            Object tokenType = claims.get(CLAIM_TOKEN_TYPE);
            return tokenType == null ? "" : String.valueOf(tokenType);
        } catch (Exception e) {
            return "";
        }
    }

    private boolean validateTokenType(String token, String expectedType) {
        if (!validateToken(token)) {
            return false;
        }
        return expectedType.equalsIgnoreCase(getTokenType(token));
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return DEFAULT_ROLE;
        }
        String normalized = role.trim().toUpperCase();
        return ("ADMIN".equals(normalized) || "USER".equals(normalized)) ? normalized : DEFAULT_ROLE;
    }

    private Jws<Claims> parseClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
    }
}
