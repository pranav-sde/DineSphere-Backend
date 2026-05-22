package com.festora.authservice.customer.validator;

import com.festora.authservice.customer.dto.SessionData;
import com.festora.authservice.repository.CustomerSessionRepository;
import com.festora.orderservice.enums.SeatingType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@RequiredArgsConstructor
public class SessionStore {

    private final CustomerSessionRepository sessionRepo;

    public SessionData get(String sessionId) {
        return sessionRepo.findBySessionId(sessionId)
                .filter(session -> session.getExpiryDate().after(new Date()))
                .map(session -> new SessionData(
                        session.getSessionId(),
                        session.getRestaurantId(),
                        session.getTableNumber(),
                        session.getSeatingType() != null
                                ? session.getSeatingType().name()
                                : SeatingType.TABLE.name()
                ))
                .orElse(null);
    }
}
