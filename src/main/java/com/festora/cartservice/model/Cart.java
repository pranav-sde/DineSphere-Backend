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
public class Cart implements Serializable {

    private String cartId;
    private Long restaurantId;
    private Integer tableNumber;
    private String userId;

    private long createdAt;
    private long updatedAt;

    private List<CartItem> items;
    private double subtotal;
}
