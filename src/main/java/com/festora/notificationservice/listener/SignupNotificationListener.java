package com.festora.notificationservice.listener;

import com.festora.authservice.dto.event.SignupNotificationEvent;
import com.festora.authservice.model.User;
import com.festora.notificationservice.service.AdminNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
@Slf4j
public class SignupNotificationListener {

    private final AdminNotificationService adminNotificationService;

    @Async
    @EventListener
    public void handleSignupEvent(SignupNotificationEvent event) {
        try {
            User user = event.getUser();
            String message = formatSignupMessage(user);
            adminNotificationService.notifyAdmin(message);
        } catch (Exception e) {
            log.error("Failed to process signup notification event", e);
        }
    }

    private String formatSignupMessage(User user) {
        StringBuilder sb = new StringBuilder();

        sb.append("🎉 *New Restaurant Signup!*\n\n");

        sb.append("👤 *Owner:* ").append(user.getOwnerName()).append("\n");
        sb.append("🍽️ *Restaurant:* ").append(user.getRestaurantName()).append("\n");
        sb.append("📧 *Email:* ").append(user.getEmail()).append("\n");
        sb.append("📞 *Phone:* ").append(user.getPhoneNumber()).append("\n");

        if (user.getAddress() != null && !user.getAddress().isEmpty()) {
            sb.append("📍 *Address:* ").append(user.getAddress()).append("\n");
        }

        if (user.getFssaiLicense() != null && !user.getFssaiLicense().isEmpty()) {
            sb.append("📋 *FSSAI:* ").append(user.getFssaiLicense()).append("\n");
        }

        if (user.getGstNumber() != null && !user.getGstNumber().isEmpty()) {
            sb.append("🧾 *GST:* ").append(user.getGstNumber()).append("\n");
        }

        sb.append("🆔 *Restaurant ID:* ").append(user.getRestaurantId()).append("\n");
        sb.append("🚚 *Delivery:* ").append(user.isEnableDelivery() ? "Enabled" : "Disabled").append("\n");

        if (user.getSubscriptionExpiry() != null) {
            String expiryDate = user.getSubscriptionExpiry()
                    .format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
            sb.append("📅 *Trial Expires:* ").append(expiryDate).append("\n");
        }

        sb.append("\n✅ Account is active with a 1-month free trial.");

        return sb.toString();
    }
}
