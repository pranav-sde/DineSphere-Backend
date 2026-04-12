package com.festora.authservice.service;

import com.festora.authservice.config.JwtProperties;
import com.festora.authservice.model.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.stereotype.Service;

import java.security.PrivateKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final JwtProperties props;
    private final PrivateKey privateKey;

    public JwtService(JwtProperties props, PrivateKey privateKey) {
        this.props = props;
        this.privateKey = privateKey;
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();

        return Jwts.builder()
                .setSubject(user.getId().toString())
                .setIssuer(props.getIssuer())
                .setAudience(props.getAudience())
                .claim("role", user.getRole().name())
                .claim("restaurantId", user.getRestaurantId())
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(Date.from(now))
                .setExpiration(
                        Date.from(now.plusSeconds(props.getAccessTokenTtlMinutes() * 60))
                )
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }
}