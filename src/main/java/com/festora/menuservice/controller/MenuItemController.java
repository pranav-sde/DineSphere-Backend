package com.festora.menuservice.controller;

import com.festora.menuservice.dto.MenuItemDto;
import com.festora.menuservice.dto.MenuItemPageResponse;
import com.festora.menuservice.dto.MenuPriceRequest;
import com.festora.menuservice.service.MenuItemService;
import com.festora.menuservice.dto.MenuItemPriceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/menu")
@RequiredArgsConstructor
@Slf4j
public class MenuItemController {

    private final MenuItemService menuItemService;

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return new ResponseEntity<>("Menu Service up", HttpStatus.OK);
    }

    @GetMapping("/customer/items-by-category")
    public ResponseEntity<MenuItemPageResponse> getItemsByCategory(
            @RequestHeader(value = "X-Restaurant-Id", required = false) Long restaurantIdHeader,
            @RequestParam(required = false) Long restaurantId,
            @RequestParam String categoryId
    ) {
        return getItems(restaurantIdHeader, restaurantId, categoryId);
    }



    @PostMapping("/internal/menu/price")
    public ResponseEntity<MenuItemPriceResponse> calculatePrice(
            @RequestBody MenuPriceRequest request
    ) {
        return ResponseEntity.ok(
                menuItemService.calculateFinalPrice(request)
        );
    }

    @GetMapping("/owner/items")
    public ResponseEntity<MenuItemPageResponse> getItemsByRestaurantId(
            @RequestHeader("X-Restaurant-Id") Long restaurantId,
            @RequestParam(required = false) String categoryId
    ) {
        if (restaurantId == null || restaurantId == 0)
            restaurantId = 101L;
        try {
            MenuItemPageResponse response = menuItemService.getMenuItemsResponse(restaurantId, categoryId);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }


    @PostMapping("/items")
    public ResponseEntity<MenuItemDto> create(
            @RequestHeader("X-Restaurant-Id") Long restaurantId,
            @RequestParam String categoryId,
            @RequestBody MenuItemDto dto
    ) {
        return ResponseEntity.ok(
                menuItemService.createMenuItem(dto, restaurantId, categoryId)
        );
    }

    @PostMapping("/items/{id}")
    public ResponseEntity<MenuItemDto> update(
            @PathVariable String id,
            @RequestBody MenuItemDto dto
    ) {
        return ResponseEntity.ok(
                menuItemService.updateMenuItem(id, dto)
        );
    }

    @PostMapping("/items/{id}/toggle")
    public ResponseEntity<Void> toggle(
            @PathVariable String id,
            @RequestParam boolean enabled
    ) {
        menuItemService.toggleMenuItem(id, enabled);
        return ResponseEntity.ok().build();
    }

    // ── Public: customers browse the full menu or filtered by category
    @GetMapping("/items")
    public ResponseEntity<MenuItemPageResponse> getItems(
            @RequestHeader(value = "X-Restaurant-Id", required = false) Long restaurantIdHeader,
            @RequestParam(required = false) Long restaurantId,
            @RequestParam(required = false) String categoryId
    ) {
        Long rid = restaurantIdHeader != null ? restaurantIdHeader : restaurantId;
        try {
            MenuItemPageResponse response = menuItemService.getMenuItemsForCustomers(rid, categoryId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Bad request for menu items: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Internal error fetching menu items: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

