package com.festora.authservice.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SessionData {
    private String sessionId;
    private Long restaurantId;
    private Integer tableNumber;
    private String seatingType;
}
