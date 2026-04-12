package com.festora.inventoryservice.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;

@Document(collection = "inventory_reservation_items")
@CompoundIndex(name = "res_item_idx", def = "{'reservationId': 1, 'inventoryItemId': 1}", unique = true)
@Data
public class InventoryReservationItem {

    @Id
    private String id; // Use an auto-gen ID, unique index handles the logic

    private String reservationId;

    private String inventoryItemId;

    private int quantity;
}