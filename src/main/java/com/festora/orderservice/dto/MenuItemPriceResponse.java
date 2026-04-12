package com.festora.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuItemPriceResponse {
    private double finalPrice;
    private String name;
    private String variantName;
    private List<String> addonNames;
}
