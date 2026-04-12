package com.festora.cartservice.dto.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Mirror of MenuItem from menu-service for Redis sync.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuItemRedis {
    private String id;
    private Long restaurantId;
    private Boolean enabled;
    private String name;
    private Double basePrice;
    private List<VariantRedis> variants;
    private List<AddonRedis> addons;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VariantRedis {
        private String id;
        private String label;
        private Double price;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddonRedis {
        private String id;
        private String name;
        private Double price;
    }
}
