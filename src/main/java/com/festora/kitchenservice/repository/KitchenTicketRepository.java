package com.festora.kitchenservice.repository;

import com.festora.kitchenservice.model.KitchenTicket;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.festora.kitchenservice.enums.TicketStatus;
import com.festora.kitchenservice.enums.KitchenStation;
import java.util.List;
import java.util.Optional;

@Repository
public interface KitchenTicketRepository extends MongoRepository<KitchenTicket, String> {
    Optional<KitchenTicket> findByTicketId(String ticketId);
    List<KitchenTicket> findByOrderIdAndRestaurantId(String orderId, Long restaurantId);
    List<KitchenTicket> findByRestaurantIdAndStatusIn(Long restaurantId, List<TicketStatus> statusList);
    List<KitchenTicket> findByRestaurantId(Long restaurantId);
    List<KitchenTicket> findByRestaurantIdAndStatus(Long restaurantId, TicketStatus status);
    List<KitchenTicket> findByRestaurantIdAndStation(Long restaurantId, KitchenStation station);
    List<KitchenTicket> findByRestaurantIdAndStatusAndStation(Long restaurantId, TicketStatus status, KitchenStation station);
    List<KitchenTicket> findByRestaurantIdAndCreatedAtBetween(Long restaurantId, long start, long end);
    List<KitchenTicket> findByRestaurantIdAndTableNumberAndStatusIn(Long restaurantId, Integer tableNumber, List<TicketStatus> statusList);
    List<KitchenTicket> findByRestaurantIdAndStatusInOrderByCreatedAtAsc(Long restaurantId, List<TicketStatus> statusList);
    List<KitchenTicket> findByRestaurantIdAndStationAndStatusInOrderByCreatedAtAsc(Long restaurantId, KitchenStation station, List<TicketStatus> statusList);
}
