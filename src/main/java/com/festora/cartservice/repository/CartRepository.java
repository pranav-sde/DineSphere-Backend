package com.festora.cartservice.repository;

import com.festora.cartservice.model.Cart;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends MongoRepository<Cart, String> {
    Optional<Cart> findByRestaurantIdAndTableNumberAndUserId(Long restaurantId, Integer tableNumber, String userId);
    void deleteByRestaurantIdAndTableNumberAndUserId(Long restaurantId, Integer tableNumber, String userId);
}
