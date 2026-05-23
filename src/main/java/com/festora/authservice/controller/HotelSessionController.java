package com.festora.authservice.controller;

import com.festora.hotelservice.dto.HotelSessionStartResponse;
import com.festora.hotelservice.model.HotelConfig;
import com.festora.hotelservice.service.HotelConfigService;
import com.festora.authservice.utils.SessionJwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Handles hotel QR scan — returns hotel + restaurant context and a lightweight session token.
 * Unlike restaurant sessions (tied to device + table), hotel sessions are stateless:
 * identity is established at order-time via mobile + room number.
 */
@RestController
@RequestMapping("/auth/session/hotel")
@RequiredArgsConstructor
@Slf4j
public class HotelSessionController {

    private static final long SESSION_TTL_MILLIS = 4 * 60 * 60 * 1000; // 4 hours — long enough for a meal session

    private final HotelConfigService hotelConfigService;
    private final SessionJwtUtil sessionJwtUtil;

    /**
     * Guest scans the hotel QR code.
     * Returns hotel + restaurant context so frontend can load the menu,
     * plus a session token so /order/hotel/create is reachable.
     */
    @PostMapping("/start")
    public ResponseEntity<?> startHotelSession(@RequestBody Map<String, String> body) {
        try {
            String qrId = body.get("qrId");
            if (qrId == null || qrId.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "qrId is required"));
            }

            HotelConfig hotel = hotelConfigService.getByQrId(qrId);

            if (!Boolean.TRUE.equals(hotel.getActive())) {
                return ResponseEntity.status(HttpStatus.GONE)
                        .body(Map.of("error", "This hotel is no longer active"));
            }

            // Generate a stateless session token (no table number — will be provided at order time)
            String sessionId = UUID.randomUUID().toString();
            String token = sessionJwtUtil.createSessionToken(
                    sessionId,
                    hotel.getRestaurantId(),
                    0,                    // tableNumber = 0 for hotel sessions
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

            HotelSessionStartResponse response = HotelSessionStartResponse.builder()
                    .restaurantId(hotel.getRestaurantId())
                    .hotelConfigId(hotel.getId())
                    .hotelName(hotel.getHotelName())
                    .hotelType(hotel.getHotelType())
                    .seatingType("HOTEL_ROOM")
                    .roomValidationEnabled(hotel.isRoomValidationEnabled())
                    .sessionToken(token)
                    .refreshToken(refreshToken)
                    .expiresIn(SESSION_TTL_MILLIS / 1000)
                    .build();

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Hotel session start failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
