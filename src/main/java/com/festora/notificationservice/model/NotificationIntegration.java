package com.festora.notificationservice.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "notification_integrations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationIntegration {

    @Id
    private String id;

    @Indexed(unique = true)
    private Long restaurantId;

    private String telegramChatId;
    private String telegramUsername;
    private boolean telegramEnabled;

    private String whatsappPhone;
    private boolean whatsappEnabled;
    
    private int whatsappMessageCount;
    private LocalDateTime whatsappLimitResetDate;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
