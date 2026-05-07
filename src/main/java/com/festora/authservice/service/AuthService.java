package com.festora.authservice.service;

import com.festora.authservice.config.JwtProperties;
import com.festora.authservice.config.LoginRateLimiter;
import com.festora.authservice.dto.AuthResponse;
import com.festora.authservice.model.User;
import com.festora.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final LoginRateLimiter rateLimiter;
    private final JwtProperties jwtProperties;

    public AuthResponse login(String email, String password) {

        rateLimiter.validateLoginAllowed(email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    rateLimiter.onLoginFailure(email);
                    return new RuntimeException("Invalid credentials");
                });

        if (!encoder.matches(password, user.getPasswordHash())) {
            rateLimiter.onLoginFailure(email);
            throw new RuntimeException("Invalid credentials");
        }

        rateLimiter.onLoginSuccess(email);

        String access = jwtService.generateAccessToken(user);
        String refresh = refreshTokenService.create(user.getId());

        boolean isSubActive = user.getSubscriptionExpiry() != null &&
                user.getSubscriptionExpiry().isAfter(java.time.LocalDateTime.now());

        return AuthResponse.builder()
                .accessToken(access)
                .refreshToken(refresh)
                .expiresIn(jwtProperties.getAccessTokenTtlMinutes() * 60)
                .subscriptionActive(isSubActive)
                .subscriptionExpiry(user.getSubscriptionExpiry())
                .build();
    }

    public AuthResponse refresh(String refreshToken) {
        String userId = refreshTokenService.validateAndConsume(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String newAccess = jwtService.generateAccessToken(user);
        String newRefresh = refreshTokenService.create(userId);

        boolean isSubActive = user.getSubscriptionExpiry() != null &&
                user.getSubscriptionExpiry().isAfter(java.time.LocalDateTime.now());

        return AuthResponse.builder()
                .accessToken(newAccess)
                .refreshToken(newRefresh)
                .expiresIn(jwtProperties.getAccessTokenTtlMinutes() * 60)
                .subscriptionActive(isSubActive)
                .subscriptionExpiry(user.getSubscriptionExpiry())
                .build();
    }

    public void logout(String refreshToken) {
        refreshTokenService.validateAndConsume(refreshToken);
    }
}
