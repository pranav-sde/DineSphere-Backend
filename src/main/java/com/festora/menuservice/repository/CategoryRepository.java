package com.festora.menuservice.repository;

import com.festora.menuservice.entity.Category;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository
        extends MongoRepository<Category, String> {
    List<Category> findByRestaurantIdOrderBySortOrderAsc(Long restaurantId);
}



