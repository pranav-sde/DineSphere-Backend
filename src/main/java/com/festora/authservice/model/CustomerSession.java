package com.festora.authservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.Map;

@Document(collection = "customer_sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerSession {

    @Id
    private String id;

    @Indexed(unique = true)
    private String sessionId;

    @Indexed
    private String deviceId;

    private Long restaurantId;
    private Integer tableNumber;

    private Date expiryDate;

    // Optional: for general purpose data
    private Map<String, Object> data;
}
