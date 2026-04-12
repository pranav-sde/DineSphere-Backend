package com.festora.menuservice.controller;

import com.festora.menuservice.dto.CategoryMenuResponse;
import com.festora.menuservice.service.MenuOverviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/menu")
@RequiredArgsConstructor
public class MenuOverviewController {

    private final MenuOverviewService menuOverviewService;

    @GetMapping("/menu/overview")
    public CategoryMenuResponse getMenuOverview(
            @RequestHeader("X-Restaurant-Id") Long restaurantId
    ) {
        return menuOverviewService.getMenuOverview(restaurantId);
    }
}
