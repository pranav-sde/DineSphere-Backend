package com.festora.orderservice.dto;

import lombok.Data;

import java.util.List;

@Data
public class ItemUpdate {
    private String menuItemId;
    private String variantId;
    private List<String> addonIds;
    private int quantity;
}
