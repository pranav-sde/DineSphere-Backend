package com.festora.orderservice.repository;

import com.festora.orderservice.model.TableRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TableRequestRepository extends MongoRepository<TableRequest, String> {
    List<TableRequest> findByRestaurantIdAndStatusOrderByCreatedAtDesc(Long restaurantId, String status);
    List<TableRequest> findByRestaurantIdAndTableNumberAndStatus(Long restaurantId, Integer tableNumber, String status);
}
