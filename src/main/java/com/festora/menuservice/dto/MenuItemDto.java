package com.festora.menuservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MenuItemDto {

    private String menuItemId;
    private String name;
    private String description;
    private String imageUrl;

    private Boolean veg;
    private Boolean enabled;

    private Double basePrice;

    private List<VariantDto> variants;
    private List<AddonDto> addons;
}
