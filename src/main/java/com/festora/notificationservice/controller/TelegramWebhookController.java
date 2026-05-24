package com.festora.notificationservice.controller;

import com.festora.notificationservice.model.ConnectionToken;
import com.festora.notificationservice.model.NotificationIntegration;
import com.festora.notificationservice.repository.ConnectionTokenRepository;
import com.festora.notificationservice.repository.NotificationIntegrationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/webhooks/telegram")
@RequiredArgsConstructor
@Slf4j
public class TelegramWebhookController {

    private final ConnectionTokenRepository tokenRepository;
    private final NotificationIntegrationRepository integrationRepository;

    @PostMapping
    public ResponseEntity<Void> handleWebhook(@RequestBody Map<String, Object> payload) {
        try {
            if (payload.containsKey("message")) {
                Map<String, Object> message = (Map<String, Object>) payload.get("message");
                String text = (String) message.get("text");
                
                if (text != null && text.startsWith("/start ")) {
                    String tokenStr = text.substring(7).trim();
                    processStartCommand(tokenStr, message);
                }
            }
        } catch (Exception e) {
            log.error("Error processing Telegram webhook: {}", e.getMessage(), e);
        }
        return ResponseEntity.ok().build(); // Always return 200 OK to Telegram
    }

    private void processStartCommand(String tokenStr, Map<String, Object> messageMap) {
        tokenRepository.findByToken(tokenStr).ifPresent(token -> {
            if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
                log.warn("Expired connection token used: {}", tokenStr);
                return;
            }

            Map<String, Object> chat = (Map<String, Object>) messageMap.get("chat");
            String chatId = String.valueOf(chat.get("id"));
            String username = (String) chat.get("username");

            NotificationIntegration integration = integrationRepository.findByRestaurantId(token.getRestaurantId())
                    .orElse(NotificationIntegration.builder()
                            .restaurantId(token.getRestaurantId())
                            .build());

            integration.setTelegramChatId(chatId);
            integration.setTelegramUsername(username);
            integration.setTelegramEnabled(true);
            
            integrationRepository.save(integration);
            
            log.info("Successfully linked Telegram for restaurant: {}", token.getRestaurantId());
            
            // In a real scenario, we might also send a confirmation message back to the user via TelegramStrategy here
            
            tokenRepository.delete(token); // Token is one-time use
        });
    }
}
