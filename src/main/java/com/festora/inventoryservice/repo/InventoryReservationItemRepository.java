package com.festora.inventoryservice.repo;

import com.festora.inventoryservice.entity.InventoryReservationItem;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryReservationItemRepository extends MongoRepository<InventoryReservationItem, String> {

    List<InventoryReservationItem> findAllByReservationId(String reservationId);

    void deleteAllByReservationId(String reservationId);
}
