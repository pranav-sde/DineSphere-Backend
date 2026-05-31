package com.festora.notificationservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Sends notifications directly to the admin (business owner) 
 * via WhatsApp and Telegram for system-level events like new signups.
 * Unlike the per-restaurant NotificationDispatcher, this targets 
 * a fixed admin phone/chat configured in application.yml.
 */
@Service
@Slf4j
public class AdminNotificationService {

    private final WebClient whatsappClient;
    private final WebClient telegramClient;

    @Value("${whatsapp.meta.token}")
    private String metaToken;

    @Value("${whatsapp.meta.phone-number-id}")
    private String phoneNumberId;

    @Value("${telegram.bot.token}")
    private String telegramBotToken;

    @Value("${admin.notification.whatsapp-phone:919307712930}")
    private String adminWhatsappPhone;

    @Value("${admin.notification.telegram-chat-id:${ADMIN_TELEGRAM_CHAT_ID:}}")
    private String adminTelegramChatId;

    @Value("${admin.notification.enabled:true}")
    private boolean enabled;

    public AdminNotificationService(WebClient.Builder webClientBuilder) {
        this.whatsappClient = webClientBuilder.baseUrl("https://graph.facebook.com/v25.0").build();
        this.telegramClient = webClientBuilder.baseUrl("https://api.telegram.org").build();
    }

    /**
     * Send a notification to the admin via all configured channels.
     */
    public void notifyAdmin(String message) {
        if (!enabled) {
            log.debug("Admin notifications are disabled");
            return;
        }

        sendWhatsApp(message);
        sendTelegram(message);
    }

    private void sendWhatsApp(String message) {
        if (metaToken == null || metaToken.isEmpty() || metaToken.contains("your-meta-token")) {
            log.warn("WhatsApp Meta token not configured, skipping admin WhatsApp notification");
            return;
        }

        if (adminWhatsappPhone == null || adminWhatsappPhone.isEmpty()) {
            log.warn("Admin WhatsApp phone not configured, skipping");
            return;
        }

        Map<String, Object> body = new HashMap<>();
        body.put("messaging_product", "whatsapp");
        body.put("to", adminWhatsappPhone);
        body.put("type", "text");

        Map<String, String> text = new HashMap<>();
        text.put("body", message);
        body.put("text", text);

        whatsappClient.post()
                .uri("/{phone-number-id}/messages", phoneNumberId)
                .header("Authorization", "Bearer " + metaToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(response ->
                        log.info("Admin WhatsApp notification sent successfully"))
                .doOnError(error ->
                        log.error("Failed to send admin WhatsApp notification: {}", error.getMessage()))
                .onErrorResume(e -> Mono.empty())
                .subscribe();
    }

    private void sendTelegram(String message) {
        if (telegramBotToken == null || telegramBotToken.isEmpty() || telegramBotToken.contains("your-telegram-bot-token")) {
            log.warn("Telegram bot token not configured, skipping admin Telegram notification");
            return;
        }

        if (adminTelegramChatId == null || adminTelegramChatId.isEmpty()) {
            log.warn("Admin Telegram chat ID not configured, skipping");
            return;
        }

        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", adminTelegramChatId);
        body.put("text", message);
        body.put("parse_mode", "Markdown");

        telegramClient.post()
                .uri("/bot{token}/sendMessage", telegramBotToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(response ->
                        log.info("Admin Telegram notification sent successfully"))
                .doOnError(error ->
                        log.error("Failed to send admin Telegram notification: {}", error.getMessage()))
                .onErrorResume(e -> Mono.empty())
                .subscribe();
    }
}
