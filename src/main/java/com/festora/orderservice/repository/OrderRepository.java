package com.festora.orderservice.repository;

import com.festora.orderservice.enums.OrderStatus;
import com.festora.orderservice.model.Order;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

import java.util.Optional;

@Repository
public interface OrderRepository extends MongoRepository<Order, String> {
    List<Order> findByStatusInAndUpdatedAtBefore(
            List<OrderStatus> statuses,
            long cutoffTime
    );

    List<Order> findOrdersByStatus(OrderStatus status);

    Order findByOrderId(String orderId);
    List<Order> findByRestaurantIdAndStatusInOrderByCreatedAtDesc(Long restaurantId, List<OrderStatus> statuses);

    List<Order> findOrdersByRestaurantIdAndTableNumberAndUserId(Long restaurantId, Integer tableNumber, String userId);

    @Query("{ 'restaurantId': ?0, 'tableNumber': ?1, $or: [ { 'userId': ?2 }, { 'deviceId': ?3 } ] }")
    List<Order> findOrdersByRestaurantIdAndTableNumberAndUserIdOrDeviceId(Long restaurantId, Integer tableNumber, String userId, String deviceId);

    List<Order> findByRestaurantIdAndCreatedAtBetween(Long restaurantId, long start, long end);
}
