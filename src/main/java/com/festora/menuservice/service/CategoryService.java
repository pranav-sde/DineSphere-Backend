package com.festora.menuservice.service;

import com.festora.menuservice.dto.CategoryDto;
import com.festora.menuservice.entity.Category;
import com.festora.menuservice.repository.CategoryRepository;
import com.festora.subscription.config.PlanFeatures;
import com.festora.subscription.service.SubscriptionFeatureService;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepo;
    private final SubscriptionFeatureService featureService;

    @Cacheable(value = "menuCache", key = "'categories:' + #restaurantId")
    public List<CategoryDto> getCategories(Long restaurantId) {

        if (restaurantId == null) {
            throw new IllegalArgumentException("restaurantId is required");
        }

        return categoryRepo
                .findByRestaurantIdOrderBySortOrderAsc(restaurantId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @CacheEvict(value = "menuCache", allEntries = true)
    public CategoryDto createCategory(Long restaurantId, CategoryDto dto) {

        if (restaurantId == null) {
            throw new IllegalArgumentException("restaurantId is required");
        }

        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new IllegalArgumentException("Category name is required");
        }

        PlanFeatures features = featureService.getFeaturesForRestaurant(restaurantId);
        int maxCategories = features.getMaxCategories();
        if (maxCategories != -1) {
            long currentCategoriesCount = categoryRepo.countByRestaurantId(restaurantId);
            if (currentCategoriesCount >= maxCategories) {
                throw new IllegalStateException("Maximum categories limit reached for this plan (" + maxCategories + "). Please upgrade your plan.");
            }
        }

        int nextOrder = (int) categoryRepo.countByRestaurantId(restaurantId) + 1;

        Category saved = categoryRepo.save(
                Category.builder()
                        .restaurantId(restaurantId)
                        .name(dto.getName().trim())
                        .description(dto.getDescription())
                        .sortOrder(nextOrder)
                        .createdAt(System.currentTimeMillis())
                        .updatedAt(System.currentTimeMillis())
                        .build()
        );

        return toDto(saved);
    }

    private CategoryDto toDto(Category c) {
        return CategoryDto.builder()
                .categoryId(c.getId())
                .name(c.getName())
                .description(c.getDescription())
                .build();
    }

    @CacheEvict(value = "menuCache", allEntries = true)
    public void removeCategory(String categoryId) throws Exception {
        if(StringUtils.isBlank(categoryId))
            throw new Exception("CategoryId is Empty");

        categoryRepo.deleteById(categoryId);
    }
}