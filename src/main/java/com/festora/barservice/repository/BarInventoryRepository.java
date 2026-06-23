package com.festora.barservice.repository;

import com.festora.barservice.model.BarInventoryItem;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BarInventoryRepository extends MongoRepository<BarInventoryItem, String> {
}
