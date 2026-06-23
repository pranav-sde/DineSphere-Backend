package com.festora.barservice.repository;

import com.festora.barservice.model.BarTicket;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.festora.kitchenservice.enums.TicketStatus;
import java.util.List;
import java.util.Optional;

@Repository
public interface BarTicketRepository extends MongoRepository<BarTicket, String> {
    Optional<BarTicket> findByTicketId(String ticketId);
    List<BarTicket> findByRestaurantIdAndStatusIn(Long restaurantId, List<TicketStatus> statuses);
}
