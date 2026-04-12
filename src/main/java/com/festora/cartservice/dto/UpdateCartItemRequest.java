package com.festora.cartservice.dto;

import lombok.Data;

@Data
public class UpdateCartItemRequest {
    private Long restaurantId;
    private Integer tableNumber;
    private String userId;
    private int quantity;
}