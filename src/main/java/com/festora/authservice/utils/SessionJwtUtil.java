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

    @Value("${app.qr.secret}")
    private String qrSecret;

    private Key key;

    @PostConstruct
    void init() {
        key = Keys.hmacShaKeyFor(qrSecret.getBytes());
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
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + 1800_000))
                .signWith(key)
                .compact();
    }
}
