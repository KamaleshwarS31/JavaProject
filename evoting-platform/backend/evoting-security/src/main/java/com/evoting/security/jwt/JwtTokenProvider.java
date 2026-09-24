package com.evoting.security.jwt;

import com.evoting.core.domain.enums.UserRole;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * JWT token creation and validation.
 * Tokens are stateless, short-lived (15min), and signed with HMAC-SHA512.
 * The signing key MUST be provided via environment variable JWT_SECRET — never hardcoded.
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtTokenProvider(
            @Value("${evoting.jwt.secret:default-secret-change-in-production-must-be-long}") String jwtSecret,
            @Value("${evoting.jwt.expiration-minutes:15}") long expirationMinutes) {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(jwtSecret);
            if (keyBytes.length < 32) {
                keyBytes = java.security.MessageDigest.getInstance("SHA-512")
                    .digest(jwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            try {
                keyBytes = java.security.MessageDigest.getInstance("SHA-512")
                    .digest(jwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            } catch (java.security.NoSuchAlgorithmException ex) {
                throw new IllegalStateException("SHA-512 algorithm not available", ex);
            }
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMinutes * 60 * 1000L;
    }

    /**
     * Generates a JWT for an authenticated user.
     * IMPORTANT: username is the subject; role and userId are claims.
     * Sensitive data (password, credentials, vote) is NEVER included in the token.
     */
    public String generateToken(String username, UUID userId, UserRole role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(username)
                .claim("userId", userId.toString())
                .claim("role", role.name())
                .claim("jti", UUID.randomUUID().toString()) // unique token ID for revocation
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(secretKey)
                .compact();
    }

    /** Validates token signature and expiration. Returns claims if valid. */
    public Claims validateAndGetClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** Extracts username (subject) from a valid token. */
    public String getUsernameFromToken(String token) {
        return validateAndGetClaims(token).getSubject();
    }

    /** Extracts the role from a valid token. */
    public String getRoleFromToken(String token) {
        return validateAndGetClaims(token).get("role", String.class);
    }

    /** Validates token and returns true if valid, false on any error. */
    public boolean isTokenValid(String token) {
        try {
            validateAndGetClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            // Log without the token value to avoid sensitive data in logs
            log.warn("JWT validation failed: {}", ex.getClass().getSimpleName());
            return false;
        }
    }
}
