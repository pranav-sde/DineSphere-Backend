package com.festora.notificationservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationSettingsResponse {
    private boolean telegramEnabled;
    private String telegramUsername;
    private boolean telegramLinked;
    
    private boolean whatsappEnabled;
    private String whatsappPhone;
    private int whatsappUsageCount;
}
