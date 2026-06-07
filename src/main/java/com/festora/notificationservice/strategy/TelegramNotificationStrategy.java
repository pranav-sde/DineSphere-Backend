package com.festora.notificationservice.strategy;

import com.festora.notificationservice.enums.NotificationChannel;
import com.festora.notificationservice.model.NotificationIntegration;
import com.festora.notificationservice.service.NotificationLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class TelegramNotificationStrategy implements NotificationStrategy {

    private final WebClient webClient;
    private final NotificationLogService logService;

    @Value("${telegram.bot.token}")
    private String botToken;

    public TelegramNotificationStrategy(WebClient.Builder webClientBuilder, NotificationLogService logService) {
        this.webClient = webClientBuilder.baseUrl("https://api.telegram.org").build();
        this.logService = logService;
    }

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.TELEGRAM;
    }

    @Override
    public void sendNotification(NotificationIntegration integration, String message) {
        if (botToken == null || botToken.isEmpty() || botToken.contains("your-telegram-bot-token")) {
            log.warn("Telegram bot token is not configured.");
            return;
        }

        String chatId = integration.getTelegramChatId();
        if (chatId == null || chatId.isEmpty()) {
            log.warn("No Telegram chat ID found for restaurant: {}", integration.getRestaurantId());
            return;
        }

        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", chatId);
        body.put("text", message);
        body.put("parse_mode", "Markdown");

        webClient.post()
                .uri("/bot{token}/sendMessage", botToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(response -> {
                    log.info("Successfully sent Telegram message to chat {}", chatId);
                    logService.logSuccess(integration.getRestaurantId(), NotificationChannel.TELEGRAM, message, response);
                })
                .doOnError(error -> {
                    log.error("Failed to send Telegram message to chat {}: {}", chatId, error.getMessage());
                    logService.logFailure(integration.getRestaurantId(), NotificationChannel.TELEGRAM, message, error.getMessage());
                })
                .onErrorResume(e -> Mono.empty())
                .subscribe();
    }
}
