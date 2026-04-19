package com.festora.menuservice.controller;

import com.festora.menuservice.dto.MenuItemDto;
import com.festora.menuservice.dto.MenuItemPageResponse;
import com.festora.menuservice.dto.MenuPriceRequest;
import com.festora.menuservice.service.MenuItemService;
import com.festora.menuservice.dto.MenuItemPriceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/menu")
@RequiredArgsConstructor
public class MenuItemController {

    private final MenuItemService menuItemService;

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return new ResponseEntity<>("Menu Service up", HttpStatus.OK);
    }

    // ── Public: customers access without JWT, restaurantId comes from query param or header
    @GetMapping("/items-by-category")
    public ResponseEntity<MenuItemPageResponse> getItemsByCategory(
            @RequestHeader(value = "X-Restaurant-Id", required = false) Long restaurantIdHeader,
            @RequestParam(required = false) Long restaurantId,
            @RequestParam String categoryId
    ) {
        Long rid = restaurantIdHeader != null ? restaurantIdHeader : restaurantId;
        try {
            MenuItemPageResponse response = menuItemService.getMenuItemsResponse(rid, categoryId);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return ResponseEntity.badRequest().build();
        }
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
            @RequestHeader("X-Restaurant-Id") Long restaurantId
    ) {
        if (restaurantId == null || restaurantId == 0)
            restaurantId = 101L;
        try {
            MenuItemPageResponse response = menuItemService.getMenuItemsResponse(restaurantId, null);
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

    // ── Public: customers browse the full menu
    @GetMapping("/items")
    public ResponseEntity<MenuItemPageResponse> getItems(
            @RequestHeader(value = "X-Restaurant-Id", required = false) Long restaurantIdHeader,
            @RequestParam(required = false) Long restaurantId
    ) {
        Long rid = restaurantIdHeader != null ? restaurantIdHeader : restaurantId;
        try {
            MenuItemPageResponse response = menuItemService.getMenuItemsResponse(rid, null);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }
}

