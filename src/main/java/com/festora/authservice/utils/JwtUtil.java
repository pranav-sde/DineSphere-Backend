package com.festora.authservice.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.Map;

@Component
public class JwtUtil {
    @Value("${app.qr.secret}")
    private String qrSecret;
    @Value("${app.qr.secret}")
    private String sessionSecret;
    private Key qrKey;
    private Key sessionKey;

    @PostConstruct
    public void init(){
        this.qrKey = Keys.hmacShaKeyFor(qrSecret.getBytes());
        this.sessionKey = Keys.hmacShaKeyFor(sessionSecret.getBytes());
    }

    // Validate QR token (throws JwtException on invalid)
    public Jws<Claims> parseAndValidateQr(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(qrKey)
                .build()
                .parseClaimsJws(token);
    }

    // Create session JWT
    public String createSessionToken(String sessionId, String restaurantId, int ttlSeconds) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setSubject(sessionId)
                .setIssuer("qr-foodies-auth")
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + ttlSeconds * 1000L))
                .addClaims(Map.of("restaurantId", restaurantId))
                .signWith(sessionKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public Jws<Claims> parseAndValidateSession(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(sessionKey)
                .build()
                .parseClaimsJws(token);
    }
}
