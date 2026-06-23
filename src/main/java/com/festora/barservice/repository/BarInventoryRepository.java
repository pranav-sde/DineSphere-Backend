package com.festora.barservice.repository;

import com.festora.barservice.model.BarInventoryItem;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BarInventoryRepository extends MongoRepository<BarInventoryItem, String> {
    List<BarInventoryItem> findByRestaurantId(Long restaurantId);
    Optional<BarInventoryItem> findByRestaurantIdAndItemName(Long restaurantId, String itemName);
}
