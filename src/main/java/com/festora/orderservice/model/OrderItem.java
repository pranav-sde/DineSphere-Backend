package com.festora.orderservice.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderItem {

    private String menuItemId;
    private String name;

    private String variantId;
    private String variantName;

    private List<String> addonIds;
    private List<String> addonNames;

    private double unitPrice;
    private int quantity;
    private double totalPrice;
}