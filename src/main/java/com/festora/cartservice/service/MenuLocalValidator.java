package com.festora.cartservice.service;

import com.festora.cartservice.dto.MenuValidationResult;
import com.festora.cartservice.dto.client.MenuItemRedis;
import com.festora.cartservice.model.AddonSnapshot;
import com.festora.cartservice.model.VariantSnapshot;
import com.festora.cartservice.repository.MenuRedisRepository;
import com.festora.cartservice.client.MenuClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MenuLocalValidator {

    private final MenuRedisRepository menuRedisRepo;
    private final MenuClient menuClient; // Keep for fallback initially

    public MenuValidationResult validate(Long restaurantId, String menuItemId, String variantId, List<String> addonIds) {
        Optional<MenuItemRedis> itemOpt = menuRedisRepo.getMenuItem(menuItemId);

        if (itemOpt.isPresent()) {
            MenuItemRedis item = itemOpt.get();
            
            // Basic validation
            if (!item.getRestaurantId().equals(restaurantId)) {
                throw new IllegalArgumentException("Menu item does not belong to this restaurant");
            }
            if (Boolean.FALSE.equals(item.getEnabled())) {
                throw new IllegalStateException("Menu item is currently unavailable");
            }

            // Price & Variant
            double variantPrice = item.getBasePrice();
            VariantSnapshot variantSnapshot = null;

            if (variantId != null) {
                MenuItemRedis.VariantRedis variant = item.getVariants().stream()
                        .filter(v -> v.getId().equals(variantId))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Invalid variant selected"));
                
                variantPrice = variant.getPrice();
                variantSnapshot = VariantSnapshot.builder()
                        .variantId(variant.getId())
                        .label(variant.getLabel())
                        .build();
            }

            // Addons
            List<AddonSnapshot> addonSnapshots = List.of();
            if (addonIds != null && !addonIds.isEmpty()) {
                addonSnapshots = item.getAddons().stream()
                        .filter(a -> addonIds.contains(a.getId()))
                        .map(a -> AddonSnapshot.builder()
                                .addonId(a.getId())
                                .name(a.getName())
                                .price(a.getPrice())
                                .build())
                        .collect(Collectors.toList());

                if (addonSnapshots.size() != addonIds.size()) {
                    throw new IllegalArgumentException("Some selected addons are invalid");
                }
            }

            return MenuValidationResult.builder()
                    .menuItemId(item.getId())
                    .itemName(item.getName())
                    .variant(variantSnapshot)
                    .variantPrice(variantPrice)
                    .addons(addonSnapshots)
                    .build();
        }

        // Fallback to HTTP if not in Redis (safeguard)
        log.warn("Menu item {} not found in Redis, falling back to HTTP call", menuItemId);
        return menuClient.validateAndFetch(restaurantId, menuItemId, variantId, addonIds);
    }
}
