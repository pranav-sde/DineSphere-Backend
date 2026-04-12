package com.festora.orderservice.model;

import com.festora.orderservice.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "orders")
public class Order {

    @Id
    private String id; // Internal MongoDB ID

    private String orderId; // Business Order ID (UUID)

    private Long restaurantId;
    private String userId;
    private String deviceId;
    private int tableNumber;

    private List<OrderItem> items;

    private double baseAmount;
    private double cgstAmount;
    private double sgstAmount;
    private double gstAmount;   // cgst + sgst
    private double discountAmount;
    private double totalAmount;

    private String inventoryReservationId;
    private long inventoryExpiresAt;

    private String reason;

    // State
    private OrderStatus status;

    // Timestamps
    private long createdAt;
    private long updatedAt;
}