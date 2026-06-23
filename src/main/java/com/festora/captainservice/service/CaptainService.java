package com.festora.captainservice.service;

import com.festora.captainservice.dto.CaptainTableView;
import com.festora.captainservice.model.CaptainAction;
import com.festora.captainservice.model.TableZone;
import com.festora.captainservice.repository.CaptainActionRepository;
import com.festora.captainservice.repository.TableZoneRepository;
import com.festora.kitchenservice.enums.TicketStatus;
import com.festora.kitchenservice.model.KitchenTicket;
import com.festora.kitchenservice.repository.KitchenTicketRepository;
import com.festora.orderservice.enums.OrderStatus;
import com.festora.orderservice.model.Order;
import com.festora.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CaptainService {

    private final TableZoneRepository zoneRepository;
    private final KitchenTicketRepository ticketRepository;
    private final OrderRepository orderRepository;
    private final CaptainActionRepository actionRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // Admin assigns tables to captain
    public TableZone createOrUpdateZone(TableZone zone) {
        long now = System.currentTimeMillis();
        if (zone.getCreatedAt() == 0) {
            zone.setCreatedAt(now);
        }
        zone.setUpdatedAt(now);
        return zoneRepository.save(zone);
    }

    // Admin views all zones for restaurant
    public List<TableZone> getZonesForRestaurant(Long restaurantId) {
        return zoneRepository.findByRestaurantId(restaurantId);
    }

    // Captain's live dashboard — all their tables with status
    public List<CaptainTableView> getLiveTablesForCaptain(Long restaurantId, String captainId) {
        TableZone zone = zoneRepository
            .findByRestaurantIdAndAssignedCaptainId(restaurantId, captainId)
            .orElseThrow(() -> new RuntimeException("No zone assigned to this captain"));

        if (zone.getTableNumbers() == null) {
            return new ArrayList<>();
        }

        return zone.getTableNumbers().stream().map(tableNo -> {
            List<Order> activeOrders = orderRepository
                .findByRestaurantIdAndTableNumberAndStatusNotIn(
                    restaurantId, tableNo,
                    List.of(OrderStatus.PAID, OrderStatus.CLOSED, OrderStatus.CANCELLED)
                );
            List<KitchenTicket> pendingTickets = ticketRepository
                .findByRestaurantIdAndTableNumberAndStatusIn(
                    restaurantId, tableNo,
                    List.of(TicketStatus.READY)
                );
            return CaptainTableView.builder()
                .tableNumber(tableNo)
                .activeOrders(activeOrders)
                .pendingPickups(pendingTickets)
                .build();
        }).collect(Collectors.toList());
    }

    // Captain marks food as served
    public void markAsServed(String ticketId, String captainId) {
        KitchenTicket ticket = ticketRepository.findByTicketId(ticketId)
            .orElseThrow(() -> new RuntimeException("Ticket not found: " + ticketId));
        ticket.setStatus(TicketStatus.PICKED_UP);
        ticket.setPickedUpAt(System.currentTimeMillis());
        ticketRepository.save(ticket);

        // Log captain action
        logAction(ticket.getRestaurantId(), captainId, ticket.getTableNumber(),
                  ticketId, "SERVED");

        // Check if all tickets for this order are now served
        List<KitchenTicket> all = ticketRepository
            .findByOrderIdAndRestaurantId(ticket.getOrderId(), ticket.getRestaurantId());
        boolean allServed = all.stream().allMatch(t -> t.getStatus() == TicketStatus.PICKED_UP);

        if (allServed) {
            Order order = orderRepository.findByOrderId(ticket.getOrderId());
            if (order != null) {
                order.setStatus(OrderStatus.SERVED);
                order.setUpdatedAt(System.currentTimeMillis());
                orderRepository.save(order);

                // Broadcast order update
                try {
                    messagingTemplate.convertAndSend("/topic/orders/" + order.getOrderId(), order);
                    messagingTemplate.convertAndSend("/topic/restaurant/" + order.getRestaurantId() + "/orders", order);
                } catch (Exception e) {
                    log.error("Failed to broadcast order update on serve: {}", e.getMessage());
                }

                // Notify admin
                try {
                    messagingTemplate.convertAndSend(
                        "/topic/admin/" + order.getRestaurantId(),
                        Map.of("type", "ORDER_SERVED", "tableNumber", order.getTableNumber(), "orderId", order.getOrderId())
                    );
                } catch (Exception e) {
                    log.error("Failed to notify admin on serve: {}", e.getMessage());
                }
            }
        }
    }

    // Customer calls captain from QR screen
    public void notifyCaptain(Long restaurantId, Integer tableNumber, String requestType) {
        try {
            messagingTemplate.convertAndSend(
                "/topic/captain/" + restaurantId,
                Map.of(
                    "type", "CAPTAIN_CALLED",
                    "tableNumber", tableNumber,
                    "requestType", requestType,  // "ASSISTANCE", "WATER", "BILL"
                    "timestamp", System.currentTimeMillis()
                )
            );
        } catch (Exception e) {
            log.error("Failed to send websocket message to captain: {}", e.getMessage());
        }

        try {
            // Also notify admin
            messagingTemplate.convertAndSend(
                "/topic/admin/" + restaurantId,
                Map.of("type", "CAPTAIN_CALL", "tableNumber", tableNumber, "requestType", requestType)
            );
        } catch (Exception e) {
            log.error("Failed to notify admin on captain call: {}", e.getMessage());
        }
    }

    // Captain sends message back to customer QR screen
    public void sendMessageToTable(Long restaurantId, Integer tableNumber, String message) {
        try {
            messagingTemplate.convertAndSend(
                "/topic/table/" + restaurantId + "/" + tableNumber,
                Map.of("type", "CAPTAIN_MESSAGE", "message", message)
            );
        } catch (Exception e) {
            log.error("Failed to send captain message to table: {}", e.getMessage());
        }
    }

    // Captain's action history (admin can view)
    public List<CaptainAction> getActions(Long restaurantId, String captainId, long from, long to) {
        if (captainId != null && !captainId.trim().isEmpty()) {
            return actionRepository.findByRestaurantIdAndCaptainIdAndTimestampBetween(restaurantId, captainId, from, to);
        } else {
            return actionRepository.findByRestaurantIdAndTimestampBetween(restaurantId, from, to);
        }
    }

    private void logAction(Long restaurantId, String captainId, Integer tableNumber,
                           String ticketId, String actionType) {
        CaptainAction action = CaptainAction.builder()
            .restaurantId(restaurantId)
            .captainId(captainId)
            .tableNumber(tableNumber)
            .ticketId(ticketId)
            .actionType(actionType)
            .timestamp(System.currentTimeMillis())
            .build();
        actionRepository.save(action);
    }
}
