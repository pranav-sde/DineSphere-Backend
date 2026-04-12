package com.festora.inventoryservice.entity;

import com.festora.inventoryservice.enums.ReservationStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;

@Document(collection = "inventory_reservations")
@Data
public class InventoryReservation {

    @Id
    private String reservationId;

    @Indexed(unique = true)
    private String orderId;

    private ReservationStatus status;

    private long expiresAt;
    private long createdAt;
}
