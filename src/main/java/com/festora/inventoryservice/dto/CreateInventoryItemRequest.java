package com.festora.inventoryservice.dto;

import lombok.Data;

@Data
public class CreateInventoryItemRequest {
    private Long restaurantId;
    private String menuItemId;
    private String variantId;

    private int totalStock;
    private boolean enabled;
}