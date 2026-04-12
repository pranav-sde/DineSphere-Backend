package com.festora.inventoryservice.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;

@Document(collection = "inventory_items")
@CompoundIndex(name = "restaurant_menu_variant_idx", def = "{'restaurantId': 1, 'menuItemId': 1, 'variantId': 1}", unique = true)
@Data
public class InventoryItem {

    @Id
    private String id; // UUID

    private Long restaurantId;

    private String menuItemId;
    private String variantId;

    // OWNER CONTROLS THIS
    private int totalStock;

    private boolean enabled;

    private long updatedAt;
}