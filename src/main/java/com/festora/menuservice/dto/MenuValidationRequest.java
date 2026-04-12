package com.festora.menuservice.dto;

import lombok.Data;

import java.util.List;

@Data
public class MenuValidationRequest {

    private Long restaurantId;
    private String menuItemId;

    // optional
    private String variantId;
    private List<String> addonIds;
}