package com.festora.inventoryservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ToggleInventoryRequest {
    private String inventoryItemId;
    private boolean enabled;
}