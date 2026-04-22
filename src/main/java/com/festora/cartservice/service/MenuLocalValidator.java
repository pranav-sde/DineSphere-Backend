package com.festora.cartservice.service;

import com.festora.cartservice.dto.MenuValidationResult;
import com.festora.cartservice.client.MenuClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MenuLocalValidator {

    private final MenuClient menuClient;

    public MenuValidationResult validate(Long restaurantId, String menuItemId, String variantId, List<String> addonIds) {
        log.info("Validating menu item {} for restaurant {} using DB (via MenuClient)", menuItemId, restaurantId);
        return menuClient.validateAndFetch(restaurantId, menuItemId, variantId, addonIds);
    }
}
