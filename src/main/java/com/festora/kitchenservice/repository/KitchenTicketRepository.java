package com.festora.kitchenservice.repository;

import com.festora.kitchenservice.model.KitchenTicket;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KitchenTicketRepository extends MongoRepository<KitchenTicket, String> {
}
