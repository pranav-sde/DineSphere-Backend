package com.festora.barservice.repository;

import com.festora.barservice.model.BarTicket;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BarTicketRepository extends MongoRepository<BarTicket, String> {
}
