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
@RequestMapping("/qr")
@RequiredArgsConstructor
public class OwnerController {

    private final OwnerService ownerService;

    @GetMapping("/tables/bulk")
    public ResponseEntity<?> generateBulk(HttpServletRequest request, @RequestParam Integer start, @RequestParam Integer end) {

        Long restaurantId = (Long) request.getAttribute("restaurantId");

        if (restaurantId == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            List<String> urls =
                    ownerService.generateUrlsInBulk(restaurantId, start, end);
            return ResponseEntity.ok(urls);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
