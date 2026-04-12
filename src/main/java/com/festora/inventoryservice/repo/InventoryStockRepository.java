package com.festora.inventoryservice.repo;

import com.festora.inventoryservice.entity.InventoryStock;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventoryStockRepository extends MongoRepository<InventoryStock, String> {

    Optional<InventoryStock> findByInventoryItemId(String inventoryItemId);
}