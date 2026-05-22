package com.festora.orderservice.dto;

import com.festora.orderservice.enums.SeatingType;
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
    private SeatingType seatingType;
    private int unbilledOrdersCount;
    private int activeBillsCount;
    private double totalUnpaidAmount;
}
