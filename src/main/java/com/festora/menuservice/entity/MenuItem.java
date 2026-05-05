package com.festora.menuservice.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("menu_items")
@CompoundIndexes({
    @CompoundIndex(name = "restaurant_category_idx", def = "{'restaurantId': 1, 'categoryId': 1, 'enabled': 1}")
})
public class MenuItem {

    @Id
    private String id;

    @Indexed
    private Long restaurantId;

    @Indexed
    private String categoryId;

    @Indexed
    private Boolean enabled;

    private String name;
    private String description;
    private String imageUrl;

    private Double basePrice;
    private Boolean veg;

    private Integer sortOrder;

    private List<Variant> variants;
    private List<Addon> addons;

    private Long createdAt;
    private Long updatedAt;
}