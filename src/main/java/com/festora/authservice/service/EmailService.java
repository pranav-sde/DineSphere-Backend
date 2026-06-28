package com.festora.authservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${resend.from-email}")
    private String fromEmail;

    @Value("${app.frontend.admin-url:https://admin.dinesphere.co}")
    private String adminFrontendUrl;

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

    /**
     * Pre-expiry subscription reminder. {@code daysLeft} is the number of whole days remaining.
     */
    @Async
    public void sendSubscriptionExpiryReminder(String to, String restaurantName, int daysLeft,
                                               LocalDateTime expiry) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject("Action needed: Your DineSphere subscription expires in " + daysLeft + " day(s)");

            String expiryDate = expiry.format(DateTimeFormatter.ofPattern("dd MMM yyyy"));

            message.setText(String.format("""
                    Hello%s,

                    Your DineSphere subscription is set to expire on %s — that's just %d day(s) away.

                    To keep your QR ordering, notifications and analytics running without
                    interruption, please renew before the expiry date.

                    Renew now: %s/billing

                    If you have any questions, just reply to this email and our team will help.

                    Best regards,
                    The DineSphere Team
                    """,
                    restaurantName != null && !restaurantName.isBlank() ? " " + restaurantName + " team" : "",
                    expiryDate,
                    daysLeft,
                    adminFrontendUrl));

            mailSender.send(message);
            log.info("Subscription expiry reminder ({}d) sent to {}", daysLeft, to);
        } catch (Exception e) {
            log.error("Failed to send subscription expiry reminder to {}. Reason: {}", to, e.getMessage());
        }
    }

    /**
     * Post-expiry notice: subscription has lapsed and access is now restricted to the Free tier.
     */
    @Async
    public void sendSubscriptionExpiredNotice(String to, String restaurantName, LocalDateTime expiry) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject("Your DineSphere subscription has expired");

            String expiryDate = expiry.format(DateTimeFormatter.ofPattern("dd MMM yyyy"));

            message.setText(String.format("""
                    Hello%s,

                    Your DineSphere subscription expired on %s. Your account has been moved to the
                    Free plan, so some features (notifications, inventory, advanced analytics, etc.)
                    are now restricted.

                    You can restore full access any time by renewing your plan:

                    Renew now: %s/billing

                    We'd love to have you back.

                    Best regards,
                    The DineSphere Team
                    """,
                    restaurantName != null && !restaurantName.isBlank() ? " " + restaurantName + " team" : "",
                    expiryDate,
                    adminFrontendUrl));

            mailSender.send(message);
            log.info("Subscription expired notice sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send subscription expired notice to {}. Reason: {}", to, e.getMessage());
        }
    }
}
