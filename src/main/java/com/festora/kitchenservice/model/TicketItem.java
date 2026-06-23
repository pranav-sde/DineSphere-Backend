package com.festora.kitchenservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketItem {
    private String menuItemId;
    private String name;
    private int quantity;
    private String variant;           // e.g. "Large", "Half"
    private List<String> addons;      // e.g. ["Extra Cheese", "No Onion"]
    private String specialNote;
}
