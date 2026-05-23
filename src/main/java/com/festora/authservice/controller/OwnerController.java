package com.festora.authservice.controller;

import com.festora.authservice.service.OwnerService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/auth/qr")
@RequiredArgsConstructor
public class OwnerController {

    private final OwnerService ownerService;

    // ── Table QR (existing) ─────────────────────────────────────────────

    @GetMapping("/tables/bulk")
    public ResponseEntity<?> generateBulk(HttpServletRequest request, @RequestParam("start") Integer start, @RequestParam("end") Integer end) {

        Long restaurantId = extractRestaurantId(request);
        if (restaurantId == null) return ResponseEntity.status(401).build();

        try {
            List<String> urls = ownerService.generateUrlsInBulk(restaurantId, start, end);
            return ResponseEntity.ok(urls);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ── Room QR (new) ───────────────────────────────────────────────────

    @GetMapping("/rooms/bulk")
    public ResponseEntity<?> generateRoomBulk(HttpServletRequest request, @RequestParam("start") Integer start, @RequestParam("end") Integer end) {

        Long restaurantId = extractRestaurantId(request);
        if (restaurantId == null) return ResponseEntity.status(401).build();

        try {
            List<String> urls = ownerService.generateRoomUrlsInBulk(restaurantId, start, end);
            return ResponseEntity.ok(urls);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ── Shared helper ───────────────────────────────────────────────────

    private Long extractRestaurantId(HttpServletRequest request) {
        String restaurantIdStr = request.getHeader("X-Restaurant-Id");
        return restaurantIdStr != null ? Long.valueOf(restaurantIdStr) : null;
    }
}
