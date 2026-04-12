package com.festora.orderservice.client;

import com.festora.menuservice.service.MenuItemService;
import com.festora.orderservice.dto.MenuItemPriceResponse;
import com.festora.orderservice.dto.MenuPriceRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Monolith implementation of MenuClient that calls MenuItemService directly.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MenuClientImpl implements MenuClient {

    private final MenuItemService menuItemService;

    @Override
    public MenuItemPriceResponse getFinalPrice(
            String menuItemId,
            String variantId,
            List<String> addonIds,
            Long restaurantId
    ) {
        log.info("Directly calling MenuItemService for item price: {}", menuItemId);

        // Map OrderService DTO to MenuService DTO
        com.festora.menuservice.dto.MenuPriceRequest menuRequest = new com.festora.menuservice.dto.MenuPriceRequest();
        menuRequest.setMenuItemId(menuItemId);
        menuRequest.setVariantId(variantId);
        menuRequest.setAddonIds(addonIds);
        menuRequest.setRestaurantId(restaurantId);

        // Call Menu Service logic
        com.festora.menuservice.dto.MenuItemPriceResponse menuResponse = menuItemService.calculateFinalPrice(menuRequest);

        // Map back to OrderService DTO
        return MenuItemPriceResponse.builder()
                .finalPrice(menuResponse.getFinalPrice())
                .name(menuResponse.getName())
                .variantName(menuResponse.getVariantName())
                .addonNames(menuResponse.getAddonNames())
                .build();
    }
}
