package com.festora.orderservice.service;

import com.festora.orderservice.model.TableRequest;
import com.festora.orderservice.repository.TableRequestRepository;
import com.festora.notificationservice.service.NotificationDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TableRequestService {
    private final TableRequestRepository repository;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationDispatcher notificationDispatcher;
    private final BillService billService;

    public TableRequest createRequest(Long restaurantId, Integer tableNumber, String userId, String type) {
        // 1. Check for duplicate pending requests
        List<TableRequest> pending = repository.findByRestaurantIdAndTableNumberAndStatus(restaurantId, tableNumber, "PENDING");
        
        boolean hasDuplicate = pending.stream().anyMatch(r -> {
            if ("BILL".equals(type)) {
                // Deduplicate BILL requests per user
                return "BILL".equals(r.getType()) && userId.equals(r.getUserId());
            } else {
                // Deduplicate CALL_WAITER/WATER requests per table
                return type.equals(r.getType());
            }
        });

        if (hasDuplicate) {
            throw new IllegalStateException("ALREADY_REQUESTED");
        }

        // 2. Automate Invoice/Bill generation if type is BILL
        if ("BILL".equals(type)) {
            try {
                billService.generateBill(restaurantId, tableNumber, userId);
            } catch (Exception e) {
                log.warn("Auto-billing generation failed for user {} at table {}: {}", userId, tableNumber, e.getMessage());
                // Throw error so customer frontend is notified they have no unbilled orders
                throw new IllegalStateException("NO_UNBILLED_ORDERS");
            }
        }

        TableRequest request = TableRequest.builder()
                .restaurantId(restaurantId)
                .tableNumber(tableNumber)
                .userId(userId)
                .type(type)
                .status("PENDING")
                .createdAt(System.currentTimeMillis())
                .updatedAt(System.currentTimeMillis())
                .build();
        
        TableRequest saved = repository.save(request);

        // 3. Broadcast WebSocket notification to dashboard
        messagingTemplate.convertAndSend("/topic/restaurant/" + restaurantId + "/requests", saved);

        // Broadcast to captain topic
        try {
            messagingTemplate.convertAndSend(
                "/topic/captain/" + restaurantId,
                Map.of(
                    "type", "CAPTAIN_CALLED",
                    "tableNumber", tableNumber,
                    "requestType", type,
                    "timestamp", System.currentTimeMillis()
                )
            );
        } catch (Exception e) {
            log.error("Failed to notify captain via websocket: {}", e.getMessage());
        }

        // 4. Dispatch to external channels (WhatsApp/Telegram)
        try {
            String readableType = type.replace("_", " ").toLowerCase();
            String msg = String.format("🛎️ Table %d has requested: %s", tableNumber, readableType);
            notificationDispatcher.dispatch(restaurantId, msg);
        } catch (Exception e) {
            log.error("Failed to send external notification for table request", e);
        }

        return saved;
    }

    public List<TableRequest> getPendingRequests(Long restaurantId) {
        return repository.findByRestaurantIdAndStatusOrderByCreatedAtDesc(restaurantId, "PENDING");
    }

    public TableRequest resolveRequest(String id) {
        TableRequest request = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));
        request.setStatus("RESOLVED");
        request.setUpdatedAt(System.currentTimeMillis());
        
        TableRequest saved = repository.save(request);

        // Notify dashboard to remove or update this item
        messagingTemplate.convertAndSend("/topic/restaurant/" + request.getRestaurantId() + "/requests", saved);
        return saved;
    }
}
