package com.festora.authservice.service;

import com.festora.authservice.dto.SessionStartRequest;
import com.festora.authservice.dto.SessionStartResponse;
import com.festora.authservice.model.CustomerSession;
import com.festora.authservice.model.QrTableMapping;
import com.festora.authservice.model.User;
import com.festora.authservice.repository.CustomerSessionRepository;
import com.festora.authservice.repository.QrTableMappingRepository;
import com.festora.authservice.repository.UserRepository;
import com.festora.authservice.utils.SessionJwtUtil;
import com.festora.hotelservice.model.HotelConfig;
import com.festora.hotelservice.repository.HotelConfigRepository;
import com.festora.orderservice.enums.SeatingType;
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
    private final HotelConfigRepository hotelConfigRepo;
    private final UserRepository userRepository;
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

        // 1. Resolve restaurant and table/hotel information
        Optional<QrTableMapping> mappingOpt = qrRepo.findByQrId(qrId);

        if (mappingOpt.isPresent()) {
            QrTableMapping mapping = mappingOpt.get();
            if (!Boolean.TRUE.equals(mapping.getActive())) {
                throw new IllegalArgumentException("QR code is inactive: " + qrId);
            }

            Long restaurantId = mapping.getRestaurantId();
            Integer tableNumber = mapping.getTableNumber();
            SeatingType seatingType = mapping.getSeatingType() != null ? mapping.getSeatingType() : SeatingType.TABLE;

            String restaurantName = userRepository.findByRestaurantId(restaurantId)
                    .map(User::getRestaurantName)
                    .orElse("DineSphere Restaurant");

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

                    return buildResponse(tableNumber, seatingType,
                            session.getSessionId(), restaurantId, deviceId, restaurantName);
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
                    .seatingType(seatingType)
                    .expiryDate(new Date(System.currentTimeMillis() + REFRESH_TTL_MILLIS))
                    .build();
            sessionRepo.save(session);

            return buildResponse(tableNumber, seatingType, sessionId, restaurantId, deviceId, restaurantName);
        } else {
            // Not in table mappings — check if it's a hotel config QR!
            HotelConfig hotel = hotelConfigRepo.findByQrId(qrId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid or inactive QR code: " + qrId));

            if (!Boolean.TRUE.equals(hotel.getActive())) {
                throw new IllegalArgumentException("This hotel is no longer active");
            }

            // Generate stateless session token (tableNumber = 0)
            String sessionId = UUID.randomUUID().toString();
            String token = sessionJwtUtil.createSessionToken(
                    sessionId,
                    hotel.getRestaurantId(),
                    0,
                    "hotel-" + hotel.getId(),
                    "HOTEL_ROOM"
            );
            String refreshToken = sessionJwtUtil.createRefreshToken(
                    sessionId,
                    hotel.getRestaurantId(),
                    0,
                    "hotel-" + hotel.getId(),
                    "HOTEL_ROOM"
            );

            return SessionStartResponse.builder()
                    .tableNumber(0)
                    .seatingType("HOTEL_ROOM")
                    .sessionToken(token)
                    .refreshToken(refreshToken)
                    .expiresIn(4 * 60 * 60) // 4 hours in seconds
                    .hotelConfigId(hotel.getId())
                    .restaurantName(hotel.getHotelName())
                    .build();
        }
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
            SeatingType seatingType = claims.get("seatingType", String.class) != null
                    ? SeatingType.valueOf(claims.get("seatingType", String.class))
                    : SeatingType.TABLE;

            CustomerSession session = sessionRepo.findBySessionId(sessionId)
                    .orElseThrow(() -> new IllegalArgumentException("Session not found"));

            if (session.getExpiryDate().before(new Date())) {
                sessionRepo.delete(session);
                throw new IllegalArgumentException("Session expired");
            }

            // Extend the expiry date further
            session.setExpiryDate(new Date(System.currentTimeMillis() + REFRESH_TTL_MILLIS));
            sessionRepo.save(session);

            String restaurantName = userRepository.findByRestaurantId(restaurantId)
                    .map(User::getRestaurantName)
                    .orElse("DineSphere Restaurant");

            return buildResponse(tableNumber, seatingType, sessionId, restaurantId, deviceId, restaurantName);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid refresh token", e);
        }
    }

    // ── Helper: builds a uniform response with tokens ──────────────────
    private SessionStartResponse buildResponse(
            Integer tableNumber, SeatingType seatingType,
            String sessionId, Long restaurantId, String deviceId, String restaurantName) {

        return new SessionStartResponse(
                tableNumber,
                seatingType.name(),
                createToken(sessionId, restaurantId, tableNumber, deviceId, seatingType),
                createRefreshToken(sessionId, restaurantId, tableNumber, deviceId, seatingType),
                SESSION_TTL_MILLIS / 1000,
                null,
                restaurantName
        );
    }

    private String createToken(String sessionId, Long restaurantId,
                               Integer tableNumber, String deviceId, SeatingType seatingType) {
        return sessionJwtUtil.createSessionToken(
                sessionId, restaurantId, tableNumber, deviceId, seatingType.name()
        );
    }

    private String createRefreshToken(String sessionId, Long restaurantId,
                                      Integer tableNumber, String deviceId, SeatingType seatingType) {
        return sessionJwtUtil.createRefreshToken(
                sessionId, restaurantId, tableNumber, deviceId, seatingType.name()
        );
    }
}