package com.festora.authservice.config;

import com.festora.authservice.exception.TooManyLoginAttemptsException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class LoginRateLimiter {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration BLOCK_DURATION = Duration.ofMinutes(15);

    private final StringRedisTemplate redis;

    public LoginRateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void validateLoginAllowed(String email) {
        String key = key(email);

        try {
            String attempts = redis.opsForValue().get(key);

            if (attempts != null && Integer.parseInt(attempts) >= MAX_ATTEMPTS) {
                throw new TooManyLoginAttemptsException();
            }

        } catch (Exception e) {
            System.out.println("Redis unavailable, skipping rate limit: {}" + e.getMessage());
        }
    }

    public void onLoginFailure(String email) {
        String key = key(email);

        try {
            Long count = redis.opsForValue().increment(key);

            if (count != null && count == 1) {
                redis.expire(key, BLOCK_DURATION);
            }

        } catch (Exception e) {
            System.out.println("Redis unavailable, skipping failure tracking");
        }
    }

    public void onLoginSuccess(String email) {
        try {
            redis.delete(key(email));
        } catch (Exception e) {
            System.out.println("Redis unavailable, skipping cleanup");
        }
    }

    private String key(String email) {
        return "login:fail:" + email.toLowerCase();
    }
}
