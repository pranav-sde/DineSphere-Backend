package com.festora.notificationservice.repository;

import com.festora.notificationservice.model.ConnectionToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConnectionTokenRepository extends MongoRepository<ConnectionToken, String> {
    Optional<ConnectionToken> findByToken(String token);
}
