package com.garv.gopay.merchant.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {

    private static final String MERCHANT_ID_CLAIM = "merchantId";
    private static final String ROLE_CLAIM = "role";

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expiration-ms}") long expirationMs) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.expirationMs = expirationMs;
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    public String generateToken(UUID merchantId, String email, String role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(email)
                .claim(MERCHANT_ID_CLAIM, merchantId.toString())
                .claim(ROLE_CLAIM, role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(signingKey)
                .compact();
    }

    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    public UUID extractMerchantId(String token) {
        return UUID.fromString(parseClaims(token).get(MERCHANT_ID_CLAIM, String.class));
    }

    public String extractRole(String token) {
        return parseClaims(token).get(ROLE_CLAIM, String.class);
    }

    // verifies the signature and expiry, throws JwtException if either fails
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

}
