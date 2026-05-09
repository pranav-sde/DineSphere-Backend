package com.festora.authservice.service;

import com.festora.monolith.util.RedisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final RedisUtils redisUtils;
    private final EmailService emailService;
    private final Random random = new Random();

    private static final String OTP_KEY_PREFIX = "OTP:";
    private static final String VERIFIED_PREFIX = "VERIFIED:";
    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int VERIFIED_EXPIRY_MINUTES = 15;

    public void generateAndSendOtp(String email) {
        String otp = String.format("%06d", random.nextInt(1000000));
        String key = OTP_KEY_PREFIX + email.toLowerCase();
        
        // Store OTP in Redis with 5 min expiry (Plain string for reliability)
        redisUtils.put(key, otp, OTP_EXPIRY_MINUTES, TimeUnit.MINUTES);
        
        // Send async email
        emailService.sendOtpEmail(email, otp);
        log.info("Generated OTP for {}", email);
    }

    public boolean verifyOtp(String email, String userOtp) {
        if (userOtp == null || userOtp.isEmpty()) return false;
        
        String key = OTP_KEY_PREFIX + email.toLowerCase();
        String storedOtp = redisUtils.get(key);
        
        if (storedOtp != null && storedOtp.equals(userOtp)) {
            redisUtils.delete(key);
            return true;
        }
        return false;
    }
}
