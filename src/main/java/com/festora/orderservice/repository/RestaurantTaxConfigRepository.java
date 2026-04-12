package com.festora.orderservice.repository;

import com.festora.orderservice.model.RestaurantTaxConfig;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RestaurantTaxConfigRepository
        extends MongoRepository<RestaurantTaxConfig, String> {

    Optional<RestaurantTaxConfig> findByRestaurantId(Long restaurantId);
}

