package com.festora.orderservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("restaurant_tax_config")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantTaxConfig {

    @Id
    private String id;

    private Long restaurantId;

    private boolean gstEnabled;

    // percentages
    private double cgstPercent;
    private double sgstPercent;
    private double igstPercent;

    private String restaurantState;
}

