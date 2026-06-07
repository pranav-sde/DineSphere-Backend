package com.festora.authservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${resend.from-email}")
    private String fromEmail;

    @Async
    public void sendOtpEmail(String to, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject("DineSphere - Your Verification Code");
            message.setText(String.format("""
                    Welcome to DineSphere Elite Network!
                    
                    Your verification code is: %s
                    This code will expire in 5 minutes.
                    
                    If you didn't request this code, please ignore this email.
                    """, otp));

            mailSender.send(message);
            log.info("OTP email sent successfully to {} via Gmail SMTP", to);
        } catch (Exception e) {
            log.error("CRITICAL: Failed to send SMTP email to {}. Reason: {}", to, e.getMessage());
        }
    }

    @Async
    public void sendPasswordResetEmail(String to, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject("DineSphere - Reset Your Password");
            message.setText(String.format("""
                    Hello,
                    
                    You requested to reset your DineSphere account password.
                    
                    Your verification code is: %s
                    This code will expire in 5 minutes.
                    
                    If you did not request a password reset, please ignore this email.
                    
                    Best regards,
                    The DineSphere Team
                    """, otp));

            mailSender.send(message);
            log.info("Password reset OTP email sent successfully to {} via Gmail SMTP", to);
        } catch (Exception e) {
            log.error("CRITICAL: Failed to send SMTP email to {}. Reason: {}", to, e.getMessage());
        }
    }
}
