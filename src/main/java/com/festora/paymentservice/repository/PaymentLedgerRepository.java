package com.festora.paymentservice.repository;

import com.festora.paymentservice.model.PaymentLedger;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentLedgerRepository extends MongoRepository<PaymentLedger, String> {
    Optional<PaymentLedger> findByOrderId(String orderId);
}
