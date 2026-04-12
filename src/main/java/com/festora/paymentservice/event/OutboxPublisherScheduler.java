package com.festora.paymentservice.event;

import com.festora.paymentservice.model.PaymentOutbox;
import com.festora.paymentservice.repository.PaymentOutboxRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OutboxPublisherScheduler {

    private final PaymentOutboxRepository outboxRepo;
    private final PaymentEventPublisher publisher;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishEvents() {

        List<PaymentOutbox> events =
                outboxRepo.findByPublishedFalse();

        for (PaymentOutbox event : events) {
            publisher.publish(event.getEventType(), event.getAggregateId(), event.getPayload());
            event.setPublished(true);
            outboxRepo.save(event);
        }
    }
}
