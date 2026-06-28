package com.festora.subscription.repository;

import com.festora.subscription.model.SubscriptionReminderLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubscriptionReminderLogRepository extends MongoRepository<SubscriptionReminderLog, String> {

    boolean existsByUserIdAndReminderType(String userId, String reminderType);

    Optional<SubscriptionReminderLog> findByUserIdAndReminderType(String userId, String reminderType);
}
