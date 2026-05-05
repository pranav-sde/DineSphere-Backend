package com.festora.cartservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemDto {
    private String imageUrl;
    private String menuItemId;
    private String variantId;
    private List<String> addonIds;
    private int quantity;
}
