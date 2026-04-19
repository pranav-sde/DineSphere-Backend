package com.festora.menuservice.controller;

import com.festora.menuservice.dto.CategoryDto;
import com.festora.menuservice.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/menu/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public List<CategoryDto> getCategories(@RequestHeader(value = "X-Restaurant-Id", required = false) Long restaurantId) {
        return categoryService.getCategories(restaurantId);
    }

    @PostMapping
    public CategoryDto createCategory(
            @RequestHeader("X-Restaurant-Id") Long restaurantId,
            @RequestBody CategoryDto dto
    ) {
        return categoryService.createCategory(restaurantId, dto);
    }
}
