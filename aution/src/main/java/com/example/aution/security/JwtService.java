package com.example.aution.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * JwtService owns every step of the JWT lifecycle:
 *  - Token generation (signing with HMAC-SHA256)
 *  - Claim extraction (username, role, userId)
 *  - Token validation (signature + expiry)
 *
 * No Spring Security magic — every line is explicit.
 * This is what FAANG interviews mean by "implement JWT from scratch".
 */
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration.ms}")
    private long expirationMs;

    // ----------------------------------------------------------------
    // Token Generation
    // ----------------------------------------------------------------

    /**
     * Generates a signed JWT for the given user.
     * Extra claims (role, userId) are embedded so every protected endpoint
     * can make role decisions without an extra DB call.
     */
    public String generateToken(UserDetails userDetails, Long userId, String role) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", role);       // e.g. "ROLE_ADMIN"
        extraClaims.put("userId", userId);   // PersonDetails.id

        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey())   // HMAC-SHA256 via SecretKey
                .compact();
    }

    // ----------------------------------------------------------------
    // Claim Extraction — each method is a focused single responsibility
    // ----------------------------------------------------------------

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    public Long extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", Long.class));
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Generic claim extractor — takes any Claims → T function.
     * This pattern lets callers pull any claim without duplicating
     * the parse/verify boilerplate.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // ----------------------------------------------------------------
    // Token Validation
    // ----------------------------------------------------------------

    /**
     * Validates token in two steps:
     *  1. Username in token matches the UserDetails subject
     *  2. Token has not expired
     *
     * Signature verification is done inside extractAllClaims() —
     * Jwts.parser() throws JwtException on any tampered token.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            String username = extractUsername(token);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            // Covers: expired, malformed, unsupported, wrong signature
            return false;
        }
    }

    // ----------------------------------------------------------------
    // Internal Helpers
    // ----------------------------------------------------------------

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Parses and verifies the token signature in one step.
     * If the secret doesn't match or the token is malformed,
     * jjwt throws a JwtException here — callers catch it.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Derives the SecretKey from the configured base64 secret string.
     * Keys.hmacShaKeyFor() ensures the key is long enough for HS256
     * (minimum 256 bits). A plain string key would be insecure.
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
