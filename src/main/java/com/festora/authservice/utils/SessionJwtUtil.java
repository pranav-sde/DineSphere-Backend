package com.festora.authservice.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class SessionJwtUtil {

    @Value("${app.session.secret}")
    private String sessionSecret;

    private Key key;

    @PostConstruct
    void init() {
        key = Keys.hmacShaKeyFor(sessionSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    public String createSessionToken(
            String sessionId,
            Long restaurantId,
            Integer table,
            String deviceId
    ) {
        long now = System.currentTimeMillis();

        return Jwts.builder()
                .claim("sid", sessionId)
                .claim("restaurantId", restaurantId)
                .claim("tableNumber", table)
                .claim("deviceId", deviceId)
                .claim("type", "access")
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + 1800_000)) // 30 minutes
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String createRefreshToken(
            String sessionId,
            Long restaurantId,
            Integer table,
            String deviceId
    ) {
        long now = System.currentTimeMillis();

        return Jwts.builder()
                .claim("sid", sessionId)
                .claim("restaurantId", restaurantId)
                .claim("tableNumber", table)
                .claim("deviceId", deviceId)
                .claim("type", "refresh")
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + 14400_000)) // 4 hours
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}
