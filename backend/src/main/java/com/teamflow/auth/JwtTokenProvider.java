package com.teamflow.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/**
 * Issues and parses Access/Refresh Tokens per 09-authentication-authorization.md §2.
 */
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final Duration accessTokenExpiry;
    private final Duration refreshTokenExpiry;

    public JwtTokenProvider(JwtProperties properties) {
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes());
        this.accessTokenExpiry = Duration.ofMinutes(properties.accessTokenExpiryMinutes());
        this.refreshTokenExpiry = Duration.ofDays(properties.refreshTokenExpiryDays());
    }

    public String generateAccessToken(Long userId, String email) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenExpiry)))
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(Long userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(refreshTokenExpiry)))
                .signWith(key)
                .compact();
    }

    public Duration getAccessTokenExpiry() {
        return accessTokenExpiry;
    }

    public Duration getRefreshTokenExpiry() {
        return refreshTokenExpiry;
    }

    /** @return parsed claims, or null if the token is missing/expired/tampered. */
    public Claims parseClaims(String token) {
        try {
            return Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    public Long getUserId(Claims claims) {
        return Long.valueOf(claims.getSubject());
    }
}
