package com.festora.inventoryservice.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;

@Document(collection = "inventory_stock")
@Data
public class InventoryStock {

    @Id
    private String inventoryItemId; // FK to InventoryItem.id

    // SYSTEM CONTROLS THESE
    private int reservedQty;
    private int confirmedQty;

    private long updatedAt;
}
