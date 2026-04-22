package com.festora.orderservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ItemUpdate {
    private String menuItemId;
    private String name;
    private String variantId;
    private List<String> addonIds;
    private List<String> addonNames;
    private int quantity;
    private int unitPrice;
    private int totalPrice;
}
