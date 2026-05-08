package com.festora.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActiveTableBillingSummary {
    private int tableNumber;
    private int unbilledOrdersCount;
    private int activeBillsCount;
    private double totalUnpaidAmount;
}
