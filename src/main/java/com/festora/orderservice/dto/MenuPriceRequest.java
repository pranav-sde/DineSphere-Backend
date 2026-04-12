package com.festora.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MenuPriceRequest {
    private String menuItemId;
    private String variantId;
    private List<String> addonIds;
    private Long restaurantId;
}
