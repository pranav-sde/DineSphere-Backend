package com.festora.notificationservice.service;

import com.festora.notificationservice.enums.NotificationChannel;
import com.festora.notificationservice.model.NotificationLog;
import com.festora.notificationservice.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NotificationLogService {
    
    private final NotificationLogRepository logRepository;

    public void logSuccess(Long restaurantId, NotificationChannel channel, String payload, String response) {
        logRepository.save(NotificationLog.builder()
                .restaurantId(restaurantId)
                .channel(channel)
                .payload(payload)
                .status("SUCCESS")
                .errorResponse(response)
                .createdAt(LocalDateTime.now())
                .build());
    }

    public void logFailure(Long restaurantId, NotificationChannel channel, String payload, String errorResponse) {
        logRepository.save(NotificationLog.builder()
                .restaurantId(restaurantId)
                .channel(channel)
                .payload(payload)
                .status("FAILED")
                .errorResponse(errorResponse)
                .createdAt(LocalDateTime.now())
                .build());
    }
}
