package com.festora.menuservice.service;

import com.festora.menuservice.dto.CategoryDto;
import com.festora.menuservice.dto.CategoryMenuResponse;
import com.festora.menuservice.dto.MenuItemDto;
import com.festora.menuservice.entity.Category;
import com.festora.menuservice.entity.MenuItem;
import com.festora.menuservice.mapper.MenuMapper;
import com.festora.menuservice.repository.CategoryRepository;
import com.festora.menuservice.repository.MenuItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MenuOverviewService {

    private static final int PREVIEW_LIMIT = 5;

    private final CategoryRepository categoryRepo;
    private final MenuItemRepository itemRepo;
    private final MenuMapper menuMapper;

    @Cacheable(
            value = "menuOverviewCache",
            key = "'menu:overview:' + #restaurantId"
    )
    public CategoryMenuResponse getMenuOverview(Long restaurantId) {

        List<Category> categories =
                categoryRepo.findByRestaurantIdOrderBySortOrderAsc(restaurantId);

        List<CategoryDto> categoryDtos =
                categories.stream()
                        .map(category -> CategoryDto.builder()
                                .categoryId(category.getId())
                                .name(category.getName())
                                .description(category.getDescription())
                                .items(
                                        loadPreviewItems(
                                                restaurantId,
                                                category.getId()
                                        )
                                )
                                .build()
                        )
                        .toList();

        return CategoryMenuResponse.builder()
                .restaurantId(restaurantId)
                .categories(categoryDtos)
                .build();
    }

    private List<MenuItemDto> loadPreviewItems(
            Long restaurantId,
            String categoryId
    ) {
       List<MenuItem> menuItems = itemRepo.findByRestaurantIdAndCategoryId(restaurantId, categoryId);
       if (CollectionUtils.isEmpty(menuItems)) {
           return new ArrayList<>();
       }
       List<MenuItemDto> menuItemDtos = new ArrayList<>();
       menuItems.forEach(menuItem -> {
           menuItemDtos.add(menuMapper.toMenuItemDto(menuItem));
       });
       return menuItemDtos;
    }
}