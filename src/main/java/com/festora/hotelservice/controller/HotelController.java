package com.festora.hotelservice.controller;

import com.festora.hotelservice.dto.CreateHotelRequest;
import com.festora.hotelservice.model.HotelConfig;
import com.festora.hotelservice.service.HotelConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth/hotel")
@RequiredArgsConstructor
public class HotelController {

    private final HotelConfigService hotelConfigService;

    @PostMapping("/create")
    public ResponseEntity<?> createHotel(
            @RequestHeader("X-Restaurant-Id") Long restaurantId,
            @RequestBody CreateHotelRequest request) {
        try {
            HotelConfig config = hotelConfigService.createHotel(restaurantId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(config);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("")
    public ResponseEntity<List<HotelConfig>> listHotels(
            @RequestHeader("X-Restaurant-Id") Long restaurantId) {
        return ResponseEntity.ok(hotelConfigService.getHotelsByRestaurant(restaurantId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateHotel(
            @PathVariable String id,
            @RequestBody CreateHotelRequest request) {
        try {
            return ResponseEntity.ok(hotelConfigService.updateHotel(id, request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/toggle")
    public ResponseEntity<HotelConfig> toggleActive(@PathVariable String id) {
        return ResponseEntity.ok(hotelConfigService.toggleActive(id));
    }

    @GetMapping("/{id}/qr")
    public ResponseEntity<Map<String, String>> getHotelQr(@PathVariable String id) {
        String url = hotelConfigService.getHotelQrUrl(id);
        return ResponseEntity.ok(Map.of("qrUrl", url));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHotel(@PathVariable String id) {
        hotelConfigService.deleteHotel(id);
        return ResponseEntity.ok().build();
    }
}