package com.festora.orderservice.gst.controller;

import com.festora.orderservice.gst.service.RestaurantTaxConfigService;
import com.festora.orderservice.model.RestaurantTaxConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tax-config")
@RequiredArgsConstructor
public class RestaurantTaxConfigController {

    private final RestaurantTaxConfigService taxConfigService;

    // CREATE or UPDATE
    @PostMapping
    public ResponseEntity<RestaurantTaxConfig> saveConfig(
            @RequestBody RestaurantTaxConfig config) {

        RestaurantTaxConfig saved = taxConfigService.saveOrUpdate(config);
        return ResponseEntity.ok(saved);
    }

    // FETCH by restaurantId
    @GetMapping("/{restaurantId}")
    public ResponseEntity<RestaurantTaxConfig> getConfig(
            @PathVariable Long restaurantId) {

        return ResponseEntity.ok(taxConfigService.getByRestaurantId(restaurantId));
    }
}

