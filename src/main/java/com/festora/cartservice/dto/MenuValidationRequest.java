package com.festora.cartservice.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MenuValidationRequest {

    private Long restaurantId;
    private String menuItemId;
    private String variantId;
    private List<String> addonIds;
}