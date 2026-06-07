package com.festora.notificationservice.strategy;

import com.festora.notificationservice.enums.NotificationChannel;
import com.festora.notificationservice.model.NotificationIntegration;

public interface NotificationStrategy {
    NotificationChannel getChannel();
    
    void sendNotification(NotificationIntegration integration, String message);
}
