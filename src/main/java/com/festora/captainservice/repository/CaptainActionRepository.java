package com.festora.captainservice.repository;

import com.festora.captainservice.model.CaptainAction;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CaptainActionRepository extends MongoRepository<CaptainAction, String> {
    List<CaptainAction> findByRestaurantIdAndCaptainIdAndTimestampBetween(Long restaurantId, String captainId, long from, long to);
    List<CaptainAction> findByRestaurantIdAndTimestampBetween(Long restaurantId, long from, long to);
}
