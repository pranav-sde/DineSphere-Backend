package com.festora.authservice.service;

import com.festora.authservice.model.RefreshToken;
import com.festora.authservice.repository.RefreshTokenRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private static final long TTL_MILLIS = 7L * 24 * 60 * 60 * 1000; // 7 days

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public String create(String userId) {
        String tokenId = UUID.randomUUID().toString();
        RefreshToken refreshToken = RefreshToken.builder()
                .token(tokenId)
                .userId(userId)
                .expiryDate(new Date(System.currentTimeMillis() + TTL_MILLIS))
                .build();
        refreshTokenRepository.save(refreshToken);
        return tokenId;
    }

    public String validateAndConsume(String tokenId) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(tokenId)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (refreshToken.getExpiryDate().before(new Date())) {
            refreshTokenRepository.deleteByToken(tokenId);
            throw new RuntimeException("Refresh token expired");
        }

        String userId = refreshToken.getUserId();
        refreshTokenRepository.deleteByToken(tokenId); // one-time use
        return userId;
    }

    public void revokeAllForUser(String userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }
}
