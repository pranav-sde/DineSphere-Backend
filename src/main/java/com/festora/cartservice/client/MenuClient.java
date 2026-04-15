package com.festora.cartservice.client;

import com.festora.menuservice.service.MenuValidationService;
import com.festora.cartservice.dto.MenuValidationResult;
import com.festora.cartservice.model.AddonSnapshot;
import com.festora.cartservice.model.VariantSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Monolith bridge for Menu Client in Cart Service.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MenuClient {

    private final MenuValidationService menuValidationService;

    public MenuValidationResult validateAndFetch(
            Long restaurantId,
            String menuItemId,
            String variantId,
            List<String> addonIds
    ) {
        log.info("Directly calling MenuValidationService for item: {}", menuItemId);

        // Map request
        com.festora.menuservice.dto.MenuValidationRequest request = new com.festora.menuservice.dto.MenuValidationRequest();
        request.setRestaurantId(restaurantId);
        request.setMenuItemId(menuItemId);
        request.setVariantId(variantId);
        request.setAddonIds(addonIds);

        // Call service
        com.festora.menuservice.dto.MenuValidationResponse response = menuValidationService.validate(request);

        // Map response back
        return MenuValidationResult.builder()
                .menuItemId(response.getMenuItemId())
                .itemName(response.getItemName())
                .variantPrice(response.getVariantPrice())
                .variant(response.getVariant() != null ? 
                    VariantSnapshot.builder()
                        .variantId(response.getVariant().getVariantId())
                        .label(response.getVariant().getLabel())
                        .build() : null)
                .addons(response.getAddons() != null ? 
                    response.getAddons().stream().map(a -> 
                        AddonSnapshot.builder()
                            .addonId(a.getAddonId())
                            .name(a.getName())
                            .price(a.getPrice())
                            .build()
                    ).collect(Collectors.toList()) : List.of())
                .build();
    }
}