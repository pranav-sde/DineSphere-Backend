package com.festora.menuservice.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MenuValidationResponse {

    private String menuItemId;
    private String itemName;

    private VariantSnapshot variant;
    private double variantPrice;

    private List<AddonSnapshot> addons;
}
