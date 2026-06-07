package com.festora.notificationservice.repository;

import com.festora.notificationservice.model.NotificationLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationLogRepository extends MongoRepository<NotificationLog, String> {
    List<NotificationLog> findByRestaurantIdOrderByCreatedAtDesc(Long restaurantId);
}
