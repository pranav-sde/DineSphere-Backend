package com.festora.inventoryservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OwnerInventoryResponse {

    // Inventory identity
    private String inventoryItemId;

    // Menu linkage (for display)
    private String menuItemId;
    private String variantId;

    // OWNER-CONTROLLED
    private int totalStock;

    // SYSTEM-CONTROLLED (READ-ONLY)
    private int reserved;
    private int confirmed;

    // DERIVED (READ-ONLY)
    private int available;

    // AVAILABILITY
    private boolean enabled;
}