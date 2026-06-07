package com.festora.notificationservice.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "connection_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConnectionToken {

    @Id
    private String id;

    @Indexed(unique = true)
    private String token;

    @Indexed
    private Long restaurantId;

    private LocalDateTime expiresAt;

    @CreatedDate
    private LocalDateTime createdAt;
}
