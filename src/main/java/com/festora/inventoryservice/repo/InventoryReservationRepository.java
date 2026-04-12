package com.festora.inventoryservice.repo;

import com.festora.inventoryservice.entity.InventoryReservation;
import com.festora.inventoryservice.enums.ReservationStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryReservationRepository extends MongoRepository<InventoryReservation, String> {

    Optional<InventoryReservation> findByOrderId(String orderId);

    List<InventoryReservation> findAllByStatusAndExpiresAtBefore(
            ReservationStatus status,
            long timestamp
    );
}