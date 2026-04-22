package com.festora.authservice.config;

import com.festora.authservice.exception.TooManyLoginAttemptsException;
import com.festora.authservice.model.LoginAttempt;
import com.festora.authservice.repository.LoginAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class LoginRateLimiter {

    private static final int MAX_ATTEMPTS = 5;
    private static final long BLOCK_DURATION_MILLIS = 15L * 60 * 1000; // 15 minutes

    private final LoginAttemptRepository attemptRepo;

    public void validateLoginAllowed(String email) {
        attemptRepo.findByEmail(email.toLowerCase()).ifPresent(attempt -> {
            if (attempt.getAttempts() >= MAX_ATTEMPTS) {
                long elapsed = System.currentTimeMillis() - attempt.getLastModified().getTime();
                if (elapsed < BLOCK_DURATION_MILLIS) {
                    throw new TooManyLoginAttemptsException();
                } else {
                    // Reset if block duration passed
                    attemptRepo.delete(attempt);
                }
            }
        });
    }

    public void onLoginFailure(String email) {
        String key = email.toLowerCase();
        LoginAttempt attempt = attemptRepo.findByEmail(key).orElse(
                LoginAttempt.builder()
                        .email(key)
                        .attempts(0)
                        .build()
        );

        attempt.setAttempts(attempt.getAttempts() + 1);
        attempt.setLastModified(new Date());
        attemptRepo.save(attempt);
    }

    public void onLoginSuccess(String email) {
        attemptRepo.deleteByEmail(email.toLowerCase());
    }
}
