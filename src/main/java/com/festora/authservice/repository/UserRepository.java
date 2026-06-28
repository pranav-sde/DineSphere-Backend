package com.festora.authservice.repository;

import com.festora.authservice.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByEmail(String email);

    Optional<User> findByRestaurantId(Long restaurantId);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    Optional<User> findTopByOrderByRestaurantIdDesc();

    /**
     * Users whose subscription expires within {@code [from, to]} — used for reminder windows.
     */
    List<User> findBySubscriptionExpiryBetween(LocalDateTime from, LocalDateTime to);

    /**
     * Users whose subscription has already lapsed but the account is still marked active —
     * candidates for hard deactivation.
     */
    List<User> findBySubscriptionExpiryBeforeAndActiveTrue(LocalDateTime cutoff);
}
