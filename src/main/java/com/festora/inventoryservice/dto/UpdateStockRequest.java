package com.festora.inventoryservice.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UpdateStockRequest {
    private String inventoryItemId;
    private int newTotalStock;
}