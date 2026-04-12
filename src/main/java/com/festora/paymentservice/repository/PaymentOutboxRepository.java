package com.festora.paymentservice.repository;

import com.festora.paymentservice.model.PaymentOutbox;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentOutboxRepository extends MongoRepository<PaymentOutbox, String> {

    List<PaymentOutbox> findByPublishedFalse();
}
