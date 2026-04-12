package com.festora.menuservice.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document("categories")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Category {

    @Id
    private String id;

    @Indexed
    private Long restaurantId;

    private String name;
    private String description;

    private Integer sortOrder;

    private Long createdAt;
    private Long updatedAt;
}
