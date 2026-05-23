package com.festora.orderservice.repository;

import com.festora.orderservice.enums.OrderStatus;
import com.festora.orderservice.enums.SeatingType;
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

    void deleteByStatusIn(List<OrderStatus> statuses);

    List<Order> findOrdersByStatus(OrderStatus status);

    Order findByOrderId(String orderId);
    List<Order> findByRestaurantIdAndStatusInOrderByCreatedAtDesc(Long restaurantId, List<OrderStatus> statuses);

    List<Order> findOrdersByRestaurantIdAndTableNumberAndUserId(Long restaurantId, Integer tableNumber, String userId);

    @Query("{ 'restaurantId': ?0, 'tableNumber': ?1, $or: [ { 'userId': ?2 }, { 'deviceId': ?3 } ] }")
    List<Order> findOrdersByRestaurantIdAndTableNumberAndUserIdOrDeviceId(Long restaurantId, Integer tableNumber, String userId, String deviceId);

    List<Order> findByRestaurantIdAndCreatedAtBetween(Long restaurantId, long start, long end);

    List<Order> findByRestaurantIdAndStatus(Long restaurantId, OrderStatus status);
    List<Order> findByRestaurantIdAndTableNumberAndStatus(Long restaurantId, Integer tableNumber, OrderStatus status);
    List<Order> findByRestaurantIdAndTableNumberAndUserIdAndStatus(Long restaurantId, Integer tableNumber, String userId, OrderStatus status);
    List<Order> findByRestaurantIdAndTableNumberAndSeatingTypeAndStatus(Long restaurantId, Integer tableNumber, SeatingType seatingType, OrderStatus status);
    List<Order> findByRestaurantIdAndTableNumberAndSeatingTypeAndUserIdAndStatus(Long restaurantId, Integer tableNumber, SeatingType seatingType, String userId, OrderStatus status);

    // Hotel room service queries
    List<Order> findByMobileNumberAndHotelConfigIdOrderByCreatedAtDesc(String mobileNumber, String hotelConfigId);
    List<Order> findByHotelConfigIdAndStatusIn(String hotelConfigId, List<OrderStatus> statuses);
    List<Order> findByRestaurantIdAndOrderSourceOrderByCreatedAtDesc(Long restaurantId, String orderSource);
    List<Order> findByRestaurantIdAndHotelConfigIdAndRoomNumberAndStatus(Long restaurantId, String hotelConfigId, String roomNumber, OrderStatus status);
    List<Order> findByRestaurantIdAndHotelConfigIdAndRoomNumberAndUserIdAndStatus(Long restaurantId, String hotelConfigId, String roomNumber, String userId, OrderStatus status);
    List<Order> findByRestaurantIdAndHotelConfigIdAndStatus(Long restaurantId, String hotelConfigId, OrderStatus status);
    List<Order> findByRestaurantIdAndHotelConfigIdAndRoomNumberAndStatusIn(Long restaurantId, String hotelConfigId, String roomNumber, List<OrderStatus> statuses);
}
