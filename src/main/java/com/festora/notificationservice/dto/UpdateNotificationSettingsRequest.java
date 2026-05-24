package com.festora.notificationservice.dto;

import lombok.Data;

@Data
public class UpdateNotificationSettingsRequest {
    private Boolean telegramEnabled;
    private Boolean whatsappEnabled;
    private String whatsappPhone;
}
