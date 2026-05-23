package com.festora.authservice.repository;

import com.festora.authservice.model.QrTableMapping;
import com.festora.orderservice.enums.SeatingType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QrTableMappingRepository extends MongoRepository<QrTableMapping, String> {
    QrTableMapping findByRestaurantIdAndTableNumber(Long restaurantId, Integer tableNumber);
    QrTableMapping findByRestaurantIdAndTableNumberAndSeatingType(Long restaurantId, Integer tableNumber, SeatingType seatingType);
    Optional<QrTableMapping> findByQrId(String qrId);
    long countByRestaurantId(Long restaurantId);
    long countByRestaurantIdAndSeatingType(Long restaurantId, SeatingType seatingType);
}
