package com.festora.menuservice.service;

import com.festora.menuservice.dto.*;
import com.festora.menuservice.entity.MenuItem;
import com.festora.menuservice.entity.Variant;
import com.festora.menuservice.repository.MenuItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuValidationService {

    private final MenuItemRepository menuItemRepo;

    public MenuValidationResponse validate(MenuValidationRequest req) {

        MenuItem item = menuItemRepo
                .findByIdAndRestaurantId(req.getMenuItemId(), req.getRestaurantId())
                .orElseThrow(() ->
                        new IllegalArgumentException("Menu item not found")
                );

        if (!Boolean.TRUE.equals(item.getEnabled())) {
            throw new IllegalStateException("Menu item disabled");
        }

        // ----- Variant -----
        VariantSnapshot variantSnapshot = null;
        double variantPrice = item.getBasePrice();

        if (req.getVariantId() != null) {
            Variant variant = item.getVariants().stream()
                    .filter(v -> v.getId().equals(req.getVariantId()))
                    .findFirst()
                    .orElseThrow(() ->
                            new IllegalArgumentException("Invalid variant")
                    );

            variantSnapshot = VariantSnapshot.builder()
                    .variantId(variant.getId())
                    .label(variant.getLabel())
                    .build();

            variantPrice = variant.getPrice();
        }

        // ----- Addons -----
        List<AddonSnapshot> addonSnapshots =
                req.getAddonIds() == null ? List.of() :
                        item.getAddons().stream()
                                .filter(a -> req.getAddonIds().contains(a.getId()))
                                .map(a -> AddonSnapshot.builder()
                                        .addonId(a.getId())
                                        .name(a.getName())
                                        .price(a.getPrice())
                                        .build())
                                .collect(Collectors.toList());

        if (req.getAddonIds() != null &&
                addonSnapshots.size() != req.getAddonIds().size()) {
            throw new IllegalArgumentException("Invalid addon");
        }

        return MenuValidationResponse.builder()
                .menuItemId(item.getId())
                .itemName(item.getName())
                .variant(variantSnapshot)
                .variantPrice(variantPrice)
                .addons(addonSnapshots)
                .build();
    }
}