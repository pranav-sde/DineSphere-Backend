package com.festora.notificationservice.model;

import com.festora.notificationservice.enums.NotificationChannel;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "notification_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationLog {

    @Id
    private String id;

    @Indexed
    private Long restaurantId;

    private NotificationChannel channel;
    
    private String payload;
    
    private String status; // SUCCESS / FAILED
    
    private String errorResponse;

    @CreatedDate
    private LocalDateTime createdAt;
}
