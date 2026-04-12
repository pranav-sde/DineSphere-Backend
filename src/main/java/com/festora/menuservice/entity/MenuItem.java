package com.festora.menuservice.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("menu_items")
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