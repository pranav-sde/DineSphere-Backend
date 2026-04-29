package com.festora.inventoryservice.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class BulkUpdateStockRequest {
    private List<BulkUpsertStockItem> items;
}
