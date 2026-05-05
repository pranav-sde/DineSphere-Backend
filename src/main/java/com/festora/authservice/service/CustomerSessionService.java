package com.festora.authservice.service;

import com.festora.authservice.dto.SessionStartRequest;
import com.festora.authservice.dto.SessionStartResponse;
import com.festora.authservice.model.CustomerSession;
import com.festora.authservice.model.QrTableMapping;
import com.festora.authservice.repository.CustomerSessionRepository;
import com.festora.authservice.repository.QrTableMappingRepository;
import com.festora.authservice.utils.SessionJwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerSessionService {

    private static final long SESSION_TTL_MILLIS = 30 * 60 * 1000; // 30 minutes
    private static final long REFRESH_TTL_MILLIS = 4 * 60 * 60 * 1000; // 4 hours

    private final QrTableMappingRepository qrRepo;
    private final CustomerSessionRepository sessionRepo;
    private final SessionJwtUtil sessionJwtUtil;
    private final com.festora.authservice.customer.validator.SessionTokenValidator tokenValidator;

    public SessionStartResponse startSession(SessionStartRequest startRequest, HttpServletRequest request) {

        final String deviceId = request.getHeader("X-Device-Id");
        if (deviceId == null || deviceId.isBlank()) {
            throw new IllegalArgumentException("Missing X-Device-Id header");
        }

        String qrId = startRequest.getQrId();
        if (ObjectUtils.isEmpty(qrId)) {
            throw new IllegalArgumentException("qrId in body must be provided");
        }

        // 1. Resolve restaurant and table information
        QrTableMapping mapping = qrRepo.findByQrId(qrId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or inactive QR code: " + qrId));

        if (!Boolean.TRUE.equals(mapping.getActive())) {
            throw new IllegalArgumentException("QR code is inactive: " + qrId);
        }

        Long restaurantId = mapping.getRestaurantId();
        Integer tableNumber = mapping.getTableNumber();

        // 2. Check for existing session for this device+table
        Optional<CustomerSession> existingSession = sessionRepo.findByDeviceIdAndRestaurantIdAndTableNumber(
                deviceId, restaurantId, tableNumber
        );

        if (existingSession.isPresent()) {
            CustomerSession session = existingSession.get();
            // Refresh expiry if not expired
            if (session.getExpiryDate().after(new Date())) {
                session.setExpiryDate(new Date(System.currentTimeMillis() + REFRESH_TTL_MILLIS));
                sessionRepo.save(session);

                return new SessionStartResponse( tableNumber,
                        createToken(session.getSessionId(), restaurantId, tableNumber, deviceId),
                        createRefreshToken(session.getSessionId(), restaurantId, tableNumber, deviceId),
                        SESSION_TTL_MILLIS / 1000
                );
            } else {
                sessionRepo.delete(session);
            }
        }

        // 3. Create new session
        String sessionId = UUID.randomUUID().toString();
        CustomerSession session = CustomerSession.builder()
                .sessionId(sessionId)
                .deviceId(deviceId)
                .restaurantId(restaurantId)
                .tableNumber(tableNumber)
                .expiryDate(new Date(System.currentTimeMillis() + REFRESH_TTL_MILLIS))
                .build();
        sessionRepo.save(session);

        return new SessionStartResponse(tableNumber,
                createToken(sessionId, restaurantId, tableNumber, deviceId),
                createRefreshToken(sessionId, restaurantId, tableNumber, deviceId),
                SESSION_TTL_MILLIS / 1000
        );
    }

    public SessionStartResponse refreshSession(String refreshToken) {
        try {
            io.jsonwebtoken.Claims claims = tokenValidator.validate(refreshToken);
            
            if (!"refresh".equals(claims.get("type"))) {
                throw new IllegalArgumentException("Invalid token type");
            }

            String sessionId = claims.get("sid", String.class);
            Long restaurantId = claims.get("restaurantId", Long.class);
            Integer tableNumber = claims.get("tableNumber", Integer.class);
            String deviceId = claims.get("deviceId", String.class);

            CustomerSession session = sessionRepo.findBySessionId(sessionId)
                    .orElseThrow(() -> new IllegalArgumentException("Session not found"));

            if (session.getExpiryDate().before(new Date())) {
                sessionRepo.delete(session);
                throw new IllegalArgumentException("Session expired");
            }

            // Extend the expiry date further
            session.setExpiryDate(new Date(System.currentTimeMillis() + REFRESH_TTL_MILLIS));
            sessionRepo.save(session);

            return new SessionStartResponse(
                    tableNumber,
                    createToken(sessionId, restaurantId, tableNumber, deviceId),
                    createRefreshToken(sessionId, restaurantId, tableNumber, deviceId),
                    SESSION_TTL_MILLIS / 1000
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid refresh token", e);
        }
    }

    private String createToken(String sessionId, Long restaurantId, Integer tableNumber, String deviceId) {
        return sessionJwtUtil.createSessionToken(
                sessionId,
                restaurantId,
                tableNumber,
                deviceId
        );
    }

    private String createRefreshToken(String sessionId, Long restaurantId, Integer tableNumber, String deviceId) {
        return sessionJwtUtil.createRefreshToken(
                sessionId,
                restaurantId,
                tableNumber,
                deviceId
        );
    }
}