package com.festora.orderservice.client;

import com.festora.orderservice.dto.MenuItemPriceResponse;

import java.util.List;

public interface MenuClient {

    MenuItemPriceResponse getFinalPrice(
            String menuItemId,
            String variantId,
            List<String> addonIds,
            Long restaurantId
    );
}
