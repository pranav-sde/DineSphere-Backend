package com.festora.cartservice.dto;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CheckoutRequest {
    private Long restaurantId;
    private Integer tableNumber;
    private String userId;
    private String deviceId;
}