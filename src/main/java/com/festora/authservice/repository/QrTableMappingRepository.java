package com.festora.authservice.repository;

import com.festora.authservice.model.QrTableMapping;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QrTableMappingRepository extends MongoRepository<QrTableMapping, String> {
    QrTableMapping findByRestaurantIdAndTableNumber(Long restaurantId, Integer tableNumber);
    Optional<QrTableMapping> findByQrId(String qrId);
    long countByRestaurantId(Long restaurantId);
}
