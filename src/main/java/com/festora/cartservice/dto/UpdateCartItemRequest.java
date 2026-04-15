package com.festora.cartservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateCartItemRequest {
    private Long restaurantId;
    private Integer tableNumber;
    private String userId;
    private int quantity;
}