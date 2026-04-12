package com.festora.inventoryservice.repo;

import com.festora.inventoryservice.entity.InventoryItem;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryItemRepository extends MongoRepository<InventoryItem, String> {

    Optional<InventoryItem> findByRestaurantIdAndMenuItemIdAndVariantId(
            Long restaurantId,
            String menuItemId,
            String variantId
    );
    List<InventoryItem> findAllByRestaurantId(Long restaurantId);
    List<InventoryItem> findAllByRestaurantIdAndMenuItemId(Long restaurantId, String menuItemId);
}
