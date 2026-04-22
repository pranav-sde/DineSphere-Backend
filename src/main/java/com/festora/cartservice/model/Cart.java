package com.festora.cartservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.util.List;

@Document(collection = "carts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndexes({
        @CompoundIndex(name = "cart_unique_idx", def = "{'restaurantId': 1, 'tableNumber': 1, 'userId': 1}", unique = true)
})
public class Cart implements Serializable {

    @Id
    private String cartId;
    private Long restaurantId;
    private Integer tableNumber;
    private String userId;

    private long createdAt;
    private long updatedAt;

    private List<CartItem> items;
    private double subtotal;
}
