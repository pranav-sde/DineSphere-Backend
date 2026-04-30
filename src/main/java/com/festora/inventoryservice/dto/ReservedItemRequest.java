package com.festora.inventoryservice.dto;

import lombok.Data;
import java.util.List;

@Data
public class ReservedItemRequest {

    private String menuItemId;
    private String variantId;
    private int quantity;
}

