package com.festora.menuservice.controller;

import com.festora.menuservice.dto.CategoryMenuResponse;
import com.festora.menuservice.service.MenuOverviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/menu/overview")
@RequiredArgsConstructor
public class MenuOverviewController {

    private final MenuOverviewService menuOverviewService;

    @GetMapping
    public CategoryMenuResponse getMenuOverview(
            @RequestHeader(value = "X-Restaurant-Id", required = false) Long restaurantIdHeader,
            @RequestParam(required = false) Long restaurantId
    ) {
        Long rid = restaurantIdHeader != null ? restaurantIdHeader : restaurantId;
        return menuOverviewService.getMenuOverview(rid);
    }
}
