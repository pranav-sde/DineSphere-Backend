package com.festora.inventoryservice.dto;

import lombok.Data;

@Data
public class ReservedItemRequest {

    private String menuItemId;
    private String variantId;
    private int quantity;
}

