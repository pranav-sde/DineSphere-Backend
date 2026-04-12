package com.festora.menuservice.dto;

import lombok.Data;

import java.util.List;

@Data
public class MenuPriceRequest {
    private String menuItemId;
    private String variantId;
    private List<String> addonIds;
    private Long restaurantId;
}

