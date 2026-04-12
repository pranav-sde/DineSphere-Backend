package com.festora.cartservice.dto;

import com.festora.cartservice.model.AddonSnapshot;
import com.festora.cartservice.model.VariantSnapshot;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MenuValidationResult {

    private String menuItemId;
    private String itemName;

    // Variant (optional)
    private VariantSnapshot variant;
    private double variantPrice;

    // Addons (optional)
    private List<AddonSnapshot> addons;
}