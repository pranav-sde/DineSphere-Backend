package com.festora.menuservice.repository;

import com.festora.menuservice.entity.MenuItem;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MenuItemRepository extends MongoRepository<MenuItem, String> {
    List<MenuItem> findByRestaurantIdAndCategoryId(Long restaurantId, String categoryId);

    Optional<MenuItem> findByIdAndRestaurantId(String id, Long restaurantId);

    List<MenuItem> findByRestaurantId(Long restaurantId);
}