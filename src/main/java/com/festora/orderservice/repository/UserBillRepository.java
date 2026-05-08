package com.festora.orderservice.repository;

import com.festora.orderservice.enums.BillingStatus;
import com.festora.orderservice.model.UserBill;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserBillRepository extends MongoRepository<UserBill, String> {
    UserBill findByBillId(String billId);
    List<UserBill> findByRestaurantIdAndTableNumberAndStatusOrderByCreatedAtDesc(Long restaurantId, int tableNumber, BillingStatus status);
    List<UserBill> findByRestaurantIdAndStatus(Long restaurantId, BillingStatus status);
    List<UserBill> findByRestaurantIdAndStatusAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(Long restaurantId, BillingStatus status, long createdAt);
    UserBill findTopByUserIdAndRestaurantIdOrderByCreatedAtDesc(String userId, Long restaurantId);
}