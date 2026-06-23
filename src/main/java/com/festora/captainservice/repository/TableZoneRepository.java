package com.festora.captainservice.repository;

import com.festora.captainservice.model.TableZone;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TableZoneRepository extends MongoRepository<TableZone, String> {
    Optional<TableZone> findByRestaurantIdAndTableNumbersContaining(Long restaurantId, Integer tableNumber);
    Optional<TableZone> findByRestaurantIdAndAssignedCaptainId(Long restaurantId, String assignedCaptainId);
    List<TableZone> findByRestaurantId(Long restaurantId);
}
