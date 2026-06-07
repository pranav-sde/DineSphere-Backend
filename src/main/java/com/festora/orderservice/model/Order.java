package com.festora.orderservice.model;

import com.festora.orderservice.enums.OrderSource;
import com.festora.orderservice.enums.OrderStatus;
import com.festora.orderservice.enums.PaymentMode;
import com.festora.orderservice.enums.SeatingType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
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

    @Indexed(unique = true)
    private String orderId; // Business Order ID (UUID)

    @Indexed
    private Long restaurantId;
    private String userId;
    private String userName;
    private String deviceId;
    private Integer tableNumber;

    // Seating & source context
    @Builder.Default
    private SeatingType seatingType = SeatingType.TABLE;
    @Builder.Default
    private OrderSource orderSource = OrderSource.DINE_IN;

    // Hotel-specific fields (null for dine-in orders)
    @Indexed
    private String hotelConfigId;
    private String hotelName;
    private String mobileNumber;
    private String roomNumber;                     // String to support "A-101", "S2" etc.
    private String restaurantMobile;

    // Payment
    @Builder.Default
    private PaymentMode paymentMode = PaymentMode.ONLINE;

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

    private String billId;
    
    // Payment Details
    private String paymentMethod;
    private String razorpayPaymentId;
    
    // State
    private OrderStatus status;

    // Timestamps
    private long createdAt;
    private long updatedAt;
}