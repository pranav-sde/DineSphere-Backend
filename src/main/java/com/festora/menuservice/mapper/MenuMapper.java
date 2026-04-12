package com.festora.menuservice.mapper;

import com.festora.menuservice.dto.*;
import com.festora.menuservice.entity.Addon;
import com.festora.menuservice.entity.Category;
import com.festora.menuservice.entity.MenuItem;
import com.festora.menuservice.entity.Variant;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class MenuMapper {

    /* ================= CATEGORY MENU ================= */

    public CategoryMenuResponse toMenuResponse(
            Long restaurantId,
            List<Category> categories,
            Map<String, List<MenuItem>> itemsByCategory
    ) {

        List<CategoryDto> categoryDtos = categories.stream()
                .map(category -> CategoryDto.builder()
                        .categoryId(category.getId())
                        .name(category.getName())
                        .description(category.getDescription())
                        .items(
                                itemsByCategory
                                        .getOrDefault(category.getId(), List.of())
                                        .stream()
                                        .filter(MenuItem::getEnabled)
                                        .map(this::toMenuItemDto)
                                        .toList()
                        )
                        .build()
                )
                .toList();

        return CategoryMenuResponse.builder()
                .restaurantId(restaurantId)
                .categories(categoryDtos)
                .build();
    }

    /* ================= MENU ITEM ================= */

    public MenuItemDto toMenuItemDto(MenuItem entity) {
        return MenuItemDto.builder()
                .menuItemId(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .imageUrl(entity.getImageUrl())
                .veg(entity.getVeg())
                .enabled(entity.getEnabled())
                .basePrice(entity.getBasePrice())
                .variants(toVariantDtos(entity.getVariants()))
                .addons(toAddonDtos(entity.getAddons()))
                .build();
    }

    public MenuItem toMenuItemEntity(MenuItemDto dto) {
        return MenuItem.builder()
                .name(dto.getName())
                .imageUrl(dto.getImageUrl())
                .description(dto.getDescription())
                .veg(dto.getVeg())
                .enabled(dto.getEnabled())
                .basePrice(dto.getBasePrice())
                .variants(toVariantEntities(dto.getVariants()))
                .addons(toAddonEntities(dto.getAddons()))
                .build();
    }

    public void updateMenuItem(MenuItem entity, MenuItemDto dto) {
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setVeg(dto.getVeg());
        entity.setBasePrice(dto.getBasePrice());
        entity.setVariants(toVariantEntities(dto.getVariants()));
        entity.setAddons(toAddonEntities(dto.getAddons()));
        entity.setImageUrl(dto.getImageUrl());
    }

    /* ================= VARIANTS ================= */

    private List<VariantDto> toVariantDtos(List<Variant> variants) {
        if (variants == null) return List.of();

        return variants.stream()
                .filter(Variant::getAvailable)
                .map(v -> VariantDto.builder()
                        .variantId(v.getId())
                        .label(v.getLabel())
                        .price(v.getPrice())
                        .available(v.getAvailable())
                        .build()
                )
                .toList();
    }

    private List<Variant> toVariantEntities(List<VariantDto> dtos) {
        if (dtos == null) return List.of();

        return dtos.stream()
                .map(v -> Variant.builder()
                        .id(
                                v.getVariantId() != null
                                        ? v.getVariantId()
                                        : "var_" + UUID.randomUUID().toString()
                        )
                        .label(v.getLabel())
                        .price(v.getPrice())
                        .available(v.getAvailable())
                        .build()
                )
                .toList();
    }

    /* ================= ADDONS ================= */

    private List<AddonDto> toAddonDtos(List<Addon> addons) {
        if (addons == null) return List.of();

        return addons.stream()
                .filter(Addon::getAvailable)
                .map(a -> AddonDto.builder()
                        .addonId(a.getId())
                        .name(a.getName())
                        .price(a.getPrice())
                        .available(a.getAvailable())
                        .build()
                )
                .toList();
    }

    private List<Addon> toAddonEntities(List<AddonDto> dtos) {
        if (dtos == null) return List.of();

        return dtos.stream()
                .map(a -> Addon.builder()
                        .id(
                                a.getAddonId() != null
                                        ? a.getAddonId()
                                        :"add_" +  UUID.randomUUID().toString()
                        )
                        .name(a.getName())
                        .price(a.getPrice())
                        .available(a.getAvailable())
                        .build()
                )
                .toList();
    }

}