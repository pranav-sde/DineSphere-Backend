package com.festora.cartservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AddToCartRequest {
    private String sessionId;
    private Long restaurantId;
    private Integer tableNumber;
    private List<CartItemDto> items;
}
