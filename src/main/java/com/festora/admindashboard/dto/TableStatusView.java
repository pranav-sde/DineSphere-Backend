package com.festora.admindashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableStatusView {
    private Integer tableNumber;
    private String status;        // "EMPTY", "OCCUPIED", "ORDERING", "IN_KITCHEN",
                                  // "READY_TO_SERVE", "BILL_REQUESTED", "PAYMENT_PENDING"
    private String color;         // "white", "green", "blue", "orange", "red", "yellow"
    private Integer activeOrders;
    private Double totalAmount;
    private Long minutesSinceLastUpdate;
    private String captainName;
    private boolean hasAlerts;    // true if waiting >20 mins or captain called
}
