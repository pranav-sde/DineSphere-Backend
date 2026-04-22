package com.festora.authservice.customer.validator;

import com.festora.authservice.customer.dto.SessionData;
import com.festora.authservice.model.CustomerSession;
import com.festora.authservice.repository.CustomerSessionRepository;
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
                        session.getTableNumber()
                ))
                .orElse(null);
    }
}
