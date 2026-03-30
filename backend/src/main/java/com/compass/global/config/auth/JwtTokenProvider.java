package com.compass.global.config.auth;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secretKey;

    private final long tokenValidTime = 30 * 60 * 1000L; // Access Token: 30분
    private final long refreshTokenValidTime = 14 * 24 * 60 * 60 * 1000L; // ★ Refresh Token: 14일

    private Key key;

    @PostConstruct
    protected void init() {
        var encodedKey = Base64.getEncoder().encodeToString(secretKey.getBytes());
        this.key = Keys.hmacShaKeyFor(encodedKey.getBytes());
    }

    // Access Token 생성
    public String createToken(String email) {
        return createJwt(email, tokenValidTime);
    }

    // ★ Refresh Token 생성
    public String createRefreshToken(String email) {
        return createJwt(email, refreshTokenValidTime);
    }

    // 공통 JWT 생성 로직
    private String createJwt(String email, long validTime) {
        var claims = Jwts.claims().setSubject(email);
        var now = new Date();
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + validTime))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Authentication getAuthentication(String token) {
        var email = this.getUserEmail(token);
        return new UsernamePasswordAuthenticationToken(email, "", Collections.emptyList());
    }

    public String getUserEmail(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody().getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}