package com.festora.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class InventoryItem {
    private String menuItemId;
    private String variantId;
    private int quantity;
}

