package com.festora.cartservice.dto.client;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class OrderItem {

    private String menuItemId;
    private String variantId;
    private List<String> addonIds;

    private double unitPrice;
    private int quantity;
    private double totalPrice;
}
