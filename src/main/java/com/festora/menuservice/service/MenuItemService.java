package com.festora.menuservice.service;

import com.festora.menuservice.dto.MenuItemDto;
import com.festora.menuservice.dto.MenuItemPageResponse;
import com.festora.menuservice.dto.MenuPriceRequest;
import com.festora.menuservice.entity.Addon;
import com.festora.menuservice.entity.MenuItem;
import com.festora.menuservice.entity.Variant;
import com.festora.menuservice.mapper.MenuMapper;
import com.festora.menuservice.repository.MenuItemRepository;
import com.festora.menuservice.dto.MenuItemPriceResponse;
import com.festora.monolith.util.RedisUtils;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import com.festora.subscription.config.PlanFeatures;
import com.festora.subscription.service.SubscriptionFeatureService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MenuItemService {

    private final MenuItemRepository itemRepo;
    private final MenuMapper menuMapper;
    private final RedisUtils redisUtils;
    private final CacheManager cacheManager;
    private final SubscriptionFeatureService featureService;

    @Cacheable(value = "menuCache", key = "'menu:' + #restaurantId + ':cat:' + #categoryId")
    public List<MenuItemDto> getMenuItemsByCategory(
            Long restaurantId,
            String categoryId
    ) {
        if (StringUtils.isBlank(categoryId)) {
            return itemRepo.findByRestaurantId(restaurantId)
                    .stream()
                    .filter(item -> Boolean.TRUE.equals(item.getEnabled()))
                    .map(menuMapper::toMenuItemDto)
                    .toList();
        }

        return itemRepo
                .findByRestaurantIdAndCategoryIdAndEnabled(restaurantId, categoryId, true)
                .stream()
                .map(menuMapper::toMenuItemDto)
                .toList();
    }

    @Cacheable(
            value = "menuCache",
            key = "'menu:' + #restaurantId"
    )
    public List<MenuItemDto> getMenuItemsByRestaurantId(
            Long restaurantId) {
        return itemRepo.findByRestaurantId(restaurantId)
                .stream()
                .filter(item -> Boolean.TRUE.equals(item.getEnabled()))
                .map(menuMapper::toMenuItemDto)
                .toList();
    }

    public MenuItemPageResponse getMenuItemsResponse(
            Long restaurantId,
            String categoryId
    ) {
        if (ObjectUtils.isEmpty(restaurantId)) {
            throw new IllegalArgumentException("RestaurantId cannot be empty");
        }

        List<MenuItemDto> items = StringUtils.isBlank(categoryId)
                ? getMenuItemsByRestaurantId(restaurantId)
                : getMenuItemsByCategory(restaurantId, categoryId);

        return MenuItemPageResponse.builder()
                .items(items)
                .totalElements(items.size())
                .build();
    }

    private void checkMenuItemLimit(Long restaurantId) {
        PlanFeatures features = featureService.getFeaturesForRestaurant(restaurantId);
        int maxItems = features.getMaxMenuItems();
        if (maxItems != -1) {
            long currentItemsCount = itemRepo.countByRestaurantIdAndEnabled(restaurantId, true);
            if (currentItemsCount >= maxItems) {
                throw new IllegalStateException("Maximum menu items limit reached for this plan (" + maxItems + "). Please upgrade your plan.");
            }
        }
    }

    @CacheEvict(value = {"menuCache", "menuPriceCache"}, allEntries = true)
    public MenuItemDto createMenuItem(
            MenuItemDto dto,
            Long restaurantId,
            String categoryId
    ) {
        checkMenuItemLimit(restaurantId);

        MenuItem entity = menuMapper.toMenuItemEntity(dto);

        entity.setRestaurantId(restaurantId);
        entity.setCategoryId(categoryId);
        entity.setEnabled(true);
        entity.setCreatedAt(System.currentTimeMillis());
        entity.setUpdatedAt(System.currentTimeMillis());

        MenuItem saved = itemRepo.save(entity);
        evictCustomerMenuCache(restaurantId);
        return menuMapper.toMenuItemDto(saved);
    }

    /**
     * UPDATE menu item
     */
    @CacheEvict(value = {"menuCache", "menuPriceCache"}, allEntries = true)
    public MenuItemDto updateMenuItem(
            String menuItemId,
            MenuItemDto dto
    ) {
        MenuItem existing = itemRepo.findById(menuItemId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Menu item not found: " + menuItemId)
                );

        menuMapper.updateMenuItem(existing, dto);
        existing.setUpdatedAt(System.currentTimeMillis());

        MenuItem saved = itemRepo.save(existing);
        evictCustomerMenuCache(saved.getRestaurantId());
        return menuMapper.toMenuItemDto(saved);
    }

    /**
     * ENABLE / DISABLE menu item (soft delete)
     */
    @CacheEvict(value = {"menuCache", "menuPriceCache"}, allEntries = true)
    public void toggleMenuItem(String menuItemId, boolean enabled) {
        MenuItem item = itemRepo.findById(menuItemId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Menu item not found: " + menuItemId)
                );

        if (enabled && !Boolean.TRUE.equals(item.getEnabled())) {
            checkMenuItemLimit(item.getRestaurantId());
        }

        item.setEnabled(enabled);
        item.setUpdatedAt(System.currentTimeMillis());
        MenuItem saved = itemRepo.save(item);
        evictCustomerMenuCache(saved.getRestaurantId());
    }

    @Cacheable(value = "menuPriceCache", key = "#request.menuItemId + ':' + #request.variantId + ':' + (#request.addonIds != null ? #request.addonIds.toString() : 'none')")
    public MenuItemPriceResponse calculateFinalPrice(MenuPriceRequest request) {

        MenuItem menuItem = itemRepo.findById(request.getMenuItemId())
                .orElseThrow(() ->
                        new IllegalArgumentException("Menu item not found: " + request.getMenuItemId())
                );

        double basePrice;
        String variantName = null;

        if (!StringUtils.isBlank(request.getVariantId())) {
            Variant variant = menuItem.getVariants().stream()
                    .filter(v -> v.getId().equals(request.getVariantId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Variant not found: " + request.getVariantId()));
            basePrice = variant.getPrice();
            variantName = variant.getLabel();
        } else {
            basePrice = menuItem.getBasePrice();
        }

        double addonsPrice = 0;
        List<String> addonNames = new ArrayList<>();

        if (!CollectionUtils.isEmpty(request.getAddonIds())) {
            for (String addonId : request.getAddonIds()) {
                // First check if they are in sub-documents of the parent item
                Optional<Addon> subAddon = Optional.ofNullable(menuItem.getAddons())
                        .orElse(List.of())
                        .stream()
                        .filter(a -> a.getId().equals(addonId))
                        .findFirst();

                if (subAddon.isPresent()) {
                    addonsPrice += subAddon.get().getPrice();
                    addonNames.add(subAddon.get().getName());
                } else {
                    // Fallback to top-level if enabled (though sub-docs preferred)
                    try {
                        MenuItem topAddon = itemRepo.findById(addonId).orElse(null);
                        if (topAddon != null) {
                            addonsPrice += topAddon.getBasePrice();
                            addonNames.add(topAddon.getName());
                        }
                    } catch (Exception ignore) {}
                }
            }
        }

        return MenuItemPriceResponse.builder()
                .finalPrice(basePrice + addonsPrice)
                .name(menuItem.getName())
                .variantName(variantName)
                .addonNames(addonNames)
                .build();
    }

    public MenuItemPageResponse getMenuItemsForCustomers(Long restaurantId, String categoryId) {
        if (restaurantId == null || restaurantId == 0)
            throw new IllegalArgumentException("Restaurant Id is Empty");

        String cacheKey = "customerMenu:" + restaurantId + ":" + (categoryId == null ? "all" : categoryId);

        // 1. Try RAM/Layer1 first (Fastest)
        var ramCache = cacheManager.getCache("menuCache");
        if (ramCache != null) {
            MenuItemPageResponse ramResult = ramCache.get(cacheKey, MenuItemPageResponse.class);
            if (ramResult != null) return ramResult;
        }

        // 2. Try Compressed Redis/ Layer2 upStash (Z-Get)
        MenuItemPageResponse cached = redisUtils.zget(cacheKey, MenuItemPageResponse.class);
        if (cached != null) {
            if (ramCache != null) ramCache.put(cacheKey, cached); // Backfill RAM
            return cached;
        }

        // 3. Database Fallback
        List<MenuItemDto> items;
        if (StringUtils.isBlank(categoryId)) {
            items = itemRepo.findByRestaurantId(restaurantId)
                    .stream()
                    .filter(item -> Boolean.TRUE.equals(item.getEnabled()))
                    .map(menuMapper::toMenuItemDto)
                    .toList();
        } else {
            items = itemRepo.findByRestaurantIdAndCategoryIdAndEnabled(restaurantId, categoryId, true)
                    .stream()
                    .map(menuMapper::toMenuItemDto)
                    .toList();
        }

        MenuItemPageResponse response = MenuItemPageResponse.builder()
                .items(items)
                .totalElements(items.size())
                .build();

        // 4. Save to both (RAM and Compressed Redis)
        if (ramCache != null) ramCache.put(cacheKey, response);
        redisUtils.zput(cacheKey, response, 24, TimeUnit.HOURS);

        return response;
    }

    public MenuItemPageResponse getMenuItemsForOwner(
            Long restaurantId,
            String categoryId
    ) {
        if (restaurantId == null || restaurantId == 0) {
            throw new IllegalArgumentException("RestaurantId cannot be empty");
        }

        List<MenuItemDto> items;
        if (StringUtils.isBlank(categoryId)) {
            items = itemRepo.findByRestaurantId(restaurantId)
                    .stream()
                    .map(menuMapper::toMenuItemDto)
                    .toList();
        } else {
            items = itemRepo.findByRestaurantIdAndCategoryId(restaurantId, categoryId)
                    .stream()
                    .map(menuMapper::toMenuItemDto)
                    .toList();
        }

        return MenuItemPageResponse.builder()
                .items(items)
                .totalElements(items.size())
                .build();
    }

    /**
     * Evicts both L1 (RAM) and L2 (Redis) customer menu caches
     * so that changes reflect immediately on the customer side.
     */
    private void evictCustomerMenuCache(Long restaurantId) {
        if (restaurantId == null) return;

        // Evict L1 RAM cache
        Cache ramCache = cacheManager.getCache("menuCache");
        if (ramCache != null) {
            ramCache.clear();
        }

        // Evict L2 compressed Redis cache (pattern-based)
        String pattern = "customerMenu:" + restaurantId + ":*";
        redisUtils.deleteKeysWithPattern(pattern);

        log.info("Evicted customer menu cache for restaurantId: {}", restaurantId);
    }
}


