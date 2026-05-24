package com.festora.notificationservice.repository;

import com.festora.notificationservice.model.NotificationIntegration;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationIntegrationRepository extends MongoRepository<NotificationIntegration, String> {
    Optional<NotificationIntegration> findByRestaurantId(Long restaurantId);
}
