package com.festora.authservice.repository;

import com.festora.authservice.model.CustomerSession;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerSessionRepository extends MongoRepository<CustomerSession, String> {
    Optional<CustomerSession> findBySessionId(String sessionId);
    Optional<CustomerSession> findByDeviceIdAndRestaurantIdAndTableNumber(String deviceId, Long restaurantId, Integer tableNumber);
    void deleteBySessionId(String sessionId);
}
