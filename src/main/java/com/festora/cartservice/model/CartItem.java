package com.festora.cartservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItem implements Serializable {

    private String cartItemId;

    private String menuItemId;
    private String imageUrl;
    private String name;

    private String identityKey;
    private VariantSnapshot variant;
    private List<AddonSnapshot> addons;

    private double unitPrice;
    private int quantity;
    private double totalPrice;
    private double gstPrice;
}
