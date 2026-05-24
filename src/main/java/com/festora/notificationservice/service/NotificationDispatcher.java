package com.festora.notificationservice.service;

import com.festora.notificationservice.enums.NotificationChannel;
import com.festora.notificationservice.model.NotificationIntegration;
import com.festora.notificationservice.repository.NotificationIntegrationRepository;
import com.festora.notificationservice.strategy.NotificationStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class NotificationDispatcher {

    private final NotificationIntegrationRepository integrationRepository;
    private final Map<NotificationChannel, NotificationStrategy> strategies;

    @Autowired
    public NotificationDispatcher(
            NotificationIntegrationRepository integrationRepository,
            List<NotificationStrategy> strategyList) {
        this.integrationRepository = integrationRepository;
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(NotificationStrategy::getChannel, Function.identity()));
    }

    public void dispatch(Long restaurantId, String message) {
        integrationRepository.findByRestaurantId(restaurantId).ifPresentOrElse(integration -> {
            
            if (integration.isTelegramEnabled() && integration.getTelegramChatId() != null) {
                dispatchToChannel(NotificationChannel.TELEGRAM, integration, message);
            }
            
            if (integration.isWhatsappEnabled() && integration.getWhatsappPhone() != null) {
                dispatchToChannel(NotificationChannel.WHATSAPP, integration, message);
            }
            
        }, () -> log.info("No notification integration found for restaurantId: {}", restaurantId));
    }

    public void dispatchToChannel(NotificationChannel channel, NotificationIntegration integration, String message) {
        NotificationStrategy strategy = strategies.get(channel);
        if (strategy != null) {
            try {
                strategy.sendNotification(integration, message);
            } catch (Exception e) {
                log.error("Failed to send notification via channel {}: {}", channel, e.getMessage());
            }
        } else {
            log.warn("No strategy found for channel: {}", channel);
        }
    }
}
