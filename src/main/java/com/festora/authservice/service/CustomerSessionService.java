package com.festora.authservice.service;

import com.festora.authservice.dto.SessionStartRequest;
import com.festora.authservice.dto.SessionStartResponse;
import com.festora.authservice.model.QrTableMapping;
import com.festora.authservice.repository.QrTableMappingRepository;
import com.festora.authservice.utils.MapperUtils;
import com.festora.authservice.utils.SessionJwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class CustomerSessionService {

    private static final long SESSION_TTL_SECONDS = 1800;

    private final QrTableMappingRepository qrRepo;
    private final StringRedisTemplate redis;
    private final SessionJwtUtil sessionJwtUtil;

    public SessionStartResponse startSession(SessionStartRequest startRequest, HttpServletRequest request) {

        final String deviceId = request.getHeader("X-Device-Id");
        if (deviceId == null || deviceId.isBlank()) {
            throw new IllegalArgumentException("Missing X-Device-Id header");
        }

        String qrId = startRequest.getQrId();
        if (ObjectUtils.isEmpty(qrId)) {
            throw new IllegalArgumentException("qrId in body must be provided");
        }

        // 1. Resolve restaurant and table information from the QR code mapping
        QrTableMapping mapping = qrRepo.findByQrId(qrId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or inactive QR code: " + qrId));

        if (!Boolean.TRUE.equals(mapping.getActive())) {
            throw new IllegalArgumentException("QR code is inactive: " + qrId);
        }

        Long restaurantId = mapping.getRestaurantId();
        Integer tableNumber = mapping.getTableNumber();

        // 2. Proceed with session creation using the resolved IDs
        final long ttl = SESSION_TTL_SECONDS;
        final Duration expiry = Duration.ofSeconds(ttl);

        final String linkKey = "QR-SESSION:" + deviceId + ":" + restaurantId + ":" + tableNumber;

        String sessionId = redis.opsForValue().get(linkKey);

        if (sessionId != null) {
            final String sessionKey = "session:" + sessionId;

            if (redis.hasKey(sessionKey)) {
                redis.expire(linkKey, expiry);
                redis.expire(sessionKey, expiry);

                Long remainingTtl = redis.getExpire(sessionKey, TimeUnit.SECONDS);
                long safeTtl = remainingTtl > 0 ? remainingTtl : ttl;

                return new SessionStartResponse(
                        createToken(sessionId, restaurantId, tableNumber, deviceId),
                        safeTtl
                );
            }
        }

        sessionId = UUID.randomUUID().toString();
        final String sessionKey = "session:" + sessionId;

        Map<String, Object> sessionData = Map.of(
                "restaurantId", restaurantId,
                "tableNumber", tableNumber,
                "deviceId", deviceId
        );

        redis.opsForValue().set(sessionKey, MapperUtils.convertObjectToString(sessionData), expiry);
        redis.opsForValue().set(linkKey, sessionId, expiry);

        return new SessionStartResponse(
                createToken(sessionId, restaurantId, tableNumber, deviceId),
                ttl
        );
    }

    private String createToken(String sessionId, Long restaurantId, Integer tableNumber, String deviceId) {
        return sessionJwtUtil.createSessionToken(
                sessionId,
                restaurantId,
                tableNumber,
                deviceId
        );
    }
}