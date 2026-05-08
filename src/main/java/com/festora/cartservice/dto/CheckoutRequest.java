package com.festora.cartservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CheckoutRequest {
    private Long restaurantId;
    private Integer tableNumber;
    private String userId;
    private String userName;
    private String deviceId;
}