package com.festora.notificationservice.controller;

import com.festora.notificationservice.dto.NotificationSettingsResponse;
import com.festora.notificationservice.dto.UpdateNotificationSettingsRequest;
import com.festora.notificationservice.model.ConnectionToken;
import com.festora.notificationservice.model.NotificationIntegration;
import com.festora.notificationservice.repository.ConnectionTokenRepository;
import com.festora.notificationservice.repository.NotificationIntegrationRepository;
import com.festora.notificationservice.service.NotificationDispatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
public class AdminNotificationController {

    private final NotificationIntegrationRepository integrationRepository;
    private final ConnectionTokenRepository tokenRepository;
    private final NotificationDispatcher dispatcher;

    @Value("${telegram.bot.username:dineSphereBot}")
    private String botUsername;

    @GetMapping("/settings/{restaurantId}")
    public ResponseEntity<NotificationSettingsResponse> getSettings(@PathVariable Long restaurantId) {
        NotificationIntegration integration = integrationRepository.findByRestaurantId(restaurantId)
                .orElse(new NotificationIntegration());

        NotificationSettingsResponse response = NotificationSettingsResponse.builder()
                .telegramEnabled(integration.isTelegramEnabled())
                .telegramUsername(integration.getTelegramUsername())
                .telegramLinked(integration.getTelegramChatId() != null)
                .whatsappEnabled(integration.isWhatsappEnabled())
                .whatsappPhone(integration.getWhatsappPhone())
                .whatsappUsageCount(integration.getWhatsappMessageCount())
                .build();

        return ResponseEntity.ok(response);
    }

    @PutMapping("/settings/{restaurantId}")
    public ResponseEntity<Void> updateSettings(@PathVariable Long restaurantId, @RequestBody UpdateNotificationSettingsRequest request) {
        NotificationIntegration integration = integrationRepository.findByRestaurantId(restaurantId)
                .orElse(NotificationIntegration.builder().restaurantId(restaurantId).build());

        if (request.getTelegramEnabled() != null) {
            integration.setTelegramEnabled(request.getTelegramEnabled());
        }
        if (request.getWhatsappEnabled() != null) {
            integration.setWhatsappEnabled(request.getWhatsappEnabled());
        }
        if (request.getWhatsappPhone() != null) {
            integration.setWhatsappPhone(request.getWhatsappPhone());
        }

        integrationRepository.save(integration);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/telegram/link/{restaurantId}")
    public ResponseEntity<Map<String, String>> generateTelegramLink(@PathVariable Long restaurantId) {
        String tokenStr = UUID.randomUUID().toString().replace("-", "");
        
        ConnectionToken token = ConnectionToken.builder()
                .token(tokenStr)
                .restaurantId(restaurantId)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();
        
        tokenRepository.save(token);
        
        String url = String.format("https://t.me/%s?start=%s", botUsername, tokenStr);
        return ResponseEntity.ok(Map.of("url", url));
    }

    @PostMapping("/telegram/unlink/{restaurantId}")
    public ResponseEntity<Void> unlinkTelegram(@PathVariable Long restaurantId) {
        integrationRepository.findByRestaurantId(restaurantId).ifPresent(integration -> {
            integration.setTelegramChatId(null);
            integration.setTelegramUsername(null);
            integration.setTelegramEnabled(false);
            integrationRepository.save(integration);
        });
        return ResponseEntity.ok().build();
    }

    @PostMapping("/test/{restaurantId}")
    public ResponseEntity<Void> triggerTestNotification(@PathVariable Long restaurantId) {
        String testMessage = "🔔 *Test Notification*\n\nThis is a test message from DineSphere to confirm your notification settings are working correctly.";
        dispatcher.dispatch(restaurantId, testMessage);
        return ResponseEntity.ok().build();
    }
}
