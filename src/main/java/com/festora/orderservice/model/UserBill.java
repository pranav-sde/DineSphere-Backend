package com.festora.orderservice.model;

import com.festora.orderservice.enums.BillingStatus;
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
@Document(collection = "user_bills")
public class UserBill {

    @Id
    private String id;

    @Indexed(unique = true)
    private String billId;

    @Indexed
    private Long restaurantId;
    
    @Indexed
    private int tableNumber;
    
    private String userId;
    private String userName;

    private List<String> orderIds;

    private List<OrderItem> items;

    private double baseAmount;
    private double cgstAmount;
    private double sgstAmount;
    private double gstAmount;
    private double discountAmount;
    private double totalAmount;

    private BillingStatus status;

    private long createdAt;
    private long updatedAt;
}
