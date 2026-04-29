package com.festora.inventoryservice.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class BulkUpsertStockItem {
    // For UPDATE: provide inventoryItemId
    private String inventoryItemId;

    // For INSERT: provide menuItemId + optional variantId
    private String menuItemId;
    private String variantId;

    // Common: stock quantity to add
    private int totalStock;

    // For INSERT: defaults to true
    private boolean enabled = true;
}
