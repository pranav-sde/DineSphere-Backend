package com.festora.kitchenservice.service;

import com.festora.captainservice.model.TableZone;
import com.festora.captainservice.repository.TableZoneRepository;
import com.festora.kitchenservice.enums.KitchenStation;
import com.festora.kitchenservice.enums.TicketStatus;
import com.festora.kitchenservice.model.KitchenTicket;
import com.festora.kitchenservice.model.TicketItem;
import com.festora.kitchenservice.repository.KitchenTicketRepository;
import com.festora.menuservice.entity.MenuItem;
import com.festora.menuservice.repository.MenuItemRepository;
import com.festora.orderservice.enums.OrderStatus;
import com.festora.orderservice.model.Order;
import com.festora.orderservice.model.OrderItem;
import com.festora.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import com.festora.barservice.service.BarService;
import com.festora.barservice.repository.BarTicketRepository;
import com.festora.barservice.model.BarTicket;

@Service
@RequiredArgsConstructor
@Slf4j
public class KitchenService {

    private final KitchenTicketRepository ticketRepository;
    private final OrderRepository orderRepository;
    private final TableZoneRepository zoneRepository;
    private final MenuItemRepository menuItemRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ApplicationEventPublisher eventPublisher;
    private final BarService barService;
    private final BarTicketRepository barTicketRepository;

    // === HOOK POINT: Called from OrderService when owner finalizes order ===
    public void createTicketsForOrder(Order order) {
        // Group order items by kitchen station
        Map<KitchenStation, List<TicketItem>> stationMap = new HashMap<>();

        for (OrderItem item : order.getItems()) {
            String categoryTag = null;
            try {
                Optional<MenuItem> menuOpt = menuItemRepository.findByIdAndRestaurantId(item.getMenuItemId(), order.getRestaurantId());
                if (menuOpt.isPresent()) {
                    categoryTag = menuOpt.get().getCategoryTag();
                }
            } catch (Exception e) {
                log.warn("Failed to find MenuItem for resolving categoryTag: itemId={}, error={}", item.getMenuItemId(), e.getMessage());
            }

            KitchenStation station = resolveStation(categoryTag);
            TicketItem ticketItem = TicketItem.builder()
                .menuItemId(item.getMenuItemId())
                .name(item.getName())
                .quantity(item.getQuantity())
                .variant(item.getVariantName())
                .addons(item.getAddonNames())
                .build();
            stationMap.computeIfAbsent(station, k -> new ArrayList<>()).add(ticketItem);
        }

        // Find captain for this table
        String captainId = resolveCaptainForTable(order.getRestaurantId(), order.getTableNumber());

        // Create one ticket per station
        for (Map.Entry<KitchenStation, List<TicketItem>> entry : stationMap.entrySet()) {
            String ticketId = UUID.randomUUID().toString();
            KitchenTicket ticket = KitchenTicket.builder()
                .ticketId(ticketId)
                .orderId(order.getOrderId())
                .restaurantId(order.getRestaurantId())
                .tableNumber(order.getTableNumber())
                .roomNumber(order.getRoomNumber())
                .station(entry.getKey())
                .items(entry.getValue())
                .status(TicketStatus.OPEN)
                .captainId(captainId)
                .createdAt(System.currentTimeMillis())
                .updatedAt(System.currentTimeMillis())
                .build();

            ticketRepository.save(ticket);

            // Deduct bar stock if BAR or LIQUOR
            if (entry.getKey() == KitchenStation.BAR || entry.getKey() == KitchenStation.LIQUOR) {
                try {
                    barService.deductBarStock(order.getRestaurantId(), entry.getValue());
                } catch (Exception e) {
                    log.error("Failed to deduct bar stock: {}", e.getMessage());
                }
            }

            // If liquor order, create BarTicket and push to liquor topic
            if (entry.getKey() == KitchenStation.LIQUOR) {
                try {
                    BarTicket barTicket = BarTicket.builder()
                        .ticketId(ticketId)
                        .orderId(order.getOrderId())
                        .restaurantId(order.getRestaurantId())
                        .tableNumber(order.getTableNumber())
                        .isLiquorOrder(true)
                        .drinks(entry.getValue())
                        .status(TicketStatus.OPEN)
                        .createdAt(System.currentTimeMillis())
                        .updatedAt(System.currentTimeMillis())
                        .build();

                    barTicketRepository.save(barTicket);

                    String liquorTopic = "/topic/kitchen/" + order.getRestaurantId() + "/liquor";
                    messagingTemplate.convertAndSend(liquorTopic, barTicket);
                } catch (Exception e) {
                    log.error("Failed to handle liquor BarTicket: {}", e.getMessage());
                }
            } else {
                // Push to correct kitchen station WebSocket channel
                String topic = "/topic/kitchen/" + order.getRestaurantId()
                             + "/" + entry.getKey().name().toLowerCase();
                try {
                    messagingTemplate.convertAndSend(topic, ticket);
                } catch (Exception e) {
                    log.error("Failed to send websocket message on topic {}: {}", topic, e.getMessage());
                }
            }
        }

        // Update order status
        order.setStatus(OrderStatus.IN_KITCHEN);
        order.setUpdatedAt(System.currentTimeMillis());
        orderRepository.save(order);

        // Broadcast order update
        try {
            messagingTemplate.convertAndSend("/topic/orders/" + order.getOrderId(), order);
            messagingTemplate.convertAndSend("/topic/restaurant/" + order.getRestaurantId() + "/orders", order);
        } catch (Exception e) {
            log.error("Failed to broadcast order update: {}", e.getMessage());
        }

        // Push to admin dashboard
        try {
            messagingTemplate.convertAndSend(
                "/topic/admin/" + order.getRestaurantId(),
                Map.of("type", "ORDER_IN_KITCHEN", "tableNumber", order.getTableNumber(), "orderId", order.getOrderId())
            );
        } catch (Exception e) {
            log.error("Failed to notify admin: {}", e.getMessage());
        }
    }

    public List<KitchenTicket> getTickets(Long restaurantId, List<TicketStatus> statuses, KitchenStation station) {
        List<TicketStatus> queryStatuses = statuses;
        if (queryStatuses == null || queryStatuses.isEmpty()) {
            queryStatuses = List.of(TicketStatus.OPEN, TicketStatus.IN_PROGRESS);
        }

        if (station != null) {
            return ticketRepository.findByRestaurantIdAndStationAndStatusInOrderByCreatedAtAsc(restaurantId, station, queryStatuses);
        } else {
            return ticketRepository.findByRestaurantIdAndStatusInOrderByCreatedAtAsc(restaurantId, queryStatuses);
        }
    }

    // === Called from Kitchen Display Screen ===
    public void markTicketInProgress(String ticketId, String staffId) {
        KitchenTicket ticket = ticketRepository.findByTicketId(ticketId)
            .orElseThrow(() -> new RuntimeException("Ticket not found: " + ticketId));

        ticket.setStatus(TicketStatus.IN_PROGRESS);
        ticket.setAssignedStaffId(staffId);
        ticket.setUpdatedAt(System.currentTimeMillis());
        ticketRepository.save(ticket);

        // Broadcast ticket update
        String topic = "/topic/kitchen/" + ticket.getRestaurantId() + "/" + ticket.getStation().name().toLowerCase();
        try {
            messagingTemplate.convertAndSend(topic, ticket);
        } catch (Exception e) {
            log.error("Failed to send websocket message on ticket: {}", e.getMessage());
        }
    }

    // === Called from Kitchen Display Screen ===
    public void markTicketReady(String ticketId) {
        KitchenTicket ticket = ticketRepository.findByTicketId(ticketId)
            .orElseThrow(() -> new RuntimeException("Ticket not found: " + ticketId));

        ticket.setStatus(TicketStatus.READY);
        ticket.setReadyAt(System.currentTimeMillis());
        ticket.setUpdatedAt(System.currentTimeMillis());
        ticketRepository.save(ticket);

        // Notify captain's phone immediately
        try {
            messagingTemplate.convertAndSend(
                "/topic/captain/" + ticket.getRestaurantId(),
                Map.of(
                    "type", "FOOD_READY",
                    "ticketId", ticketId,
                    "tableNumber", ticket.getTableNumber() != null ? ticket.getTableNumber() : 0,
                    "roomNumber", ticket.getRoomNumber() != null ? ticket.getRoomNumber() : "",
                    "station", ticket.getStation().name(),
                    "items", ticket.getItems().stream().map(TicketItem::getName).collect(Collectors.toList()),
                    "captainId", ticket.getCaptainId() != null ? ticket.getCaptainId() : ""
                )
            );
        } catch (Exception e) {
            log.error("Failed to notify captain: {}", e.getMessage());
        }

        // Check if ALL tickets for this order are READY
        checkAllTicketsReady(ticket.getOrderId(), ticket.getRestaurantId());
    }

    private void checkAllTicketsReady(String orderId, Long restaurantId) {
        List<KitchenTicket> allTickets = ticketRepository
            .findByOrderIdAndRestaurantId(orderId, restaurantId);

        boolean allReady = allTickets.stream()
            .allMatch(t -> t.getStatus() == TicketStatus.READY
                       || t.getStatus() == TicketStatus.PICKED_UP);

        if (allReady) {
            Order order = orderRepository.findByOrderId(orderId);
            if (order != null) {
                order.setStatus(OrderStatus.READY_TO_SERVE);
                order.setUpdatedAt(System.currentTimeMillis());
                orderRepository.save(order);

                // Broadcast order update
                try {
                    messagingTemplate.convertAndSend("/topic/orders/" + order.getOrderId(), order);
                    messagingTemplate.convertAndSend("/topic/restaurant/" + order.getRestaurantId() + "/orders", order);
                } catch (Exception e) {
                    log.error("Failed to broadcast order update: {}", e.getMessage());
                }

                // Notify customer QR screen
                try {
                    messagingTemplate.convertAndSend(
                        "/topic/table/" + restaurantId + "/" + order.getTableNumber(),
                        Map.of("type", "ORDER_READY", "message", "Your food is almost here! 🍽️")
                    );
                } catch (Exception e) {
                    log.error("Failed to notify customer table: {}", e.getMessage());
                }

                // Notify admin
                try {
                    messagingTemplate.convertAndSend(
                        "/topic/admin/" + restaurantId,
                        Map.of("type", "ORDER_READY_TO_SERVE", "tableNumber", order.getTableNumber())
                    );
                } catch (Exception e) {
                    log.error("Failed to notify admin: {}", e.getMessage());
                }
            }
        }
    }

    // === Auto-resolve kitchen station from category tag on MenuItem ===
    private KitchenStation resolveStation(String categoryTag) {
        if (categoryTag == null) return KitchenStation.HOT;
        return switch (categoryTag.toLowerCase()) {
            case "drinks", "beverages", "mocktails", "juice", "soft drinks" -> KitchenStation.BAR;
            case "liquor", "alcohol", "beer", "wine", "whiskey", "rum", "cocktails" -> KitchenStation.LIQUOR;
            case "salad", "cold starters", "raita", "cold" -> KitchenStation.COLD;
            case "dessert", "sweets", "ice cream", "mithai" -> KitchenStation.DESSERT;
            default -> KitchenStation.HOT;
        };
    }

    private String resolveCaptainForTable(Long restaurantId, Integer tableNumber) {
        try {
            return zoneRepository.findByRestaurantIdAndTableNumbersContaining(restaurantId, tableNumber)
                .map(TableZone::getAssignedCaptainId)
                .orElse(null);
        } catch (Exception e) {
            log.warn("Failed to resolve captain for table: restaurantId={}, table={}, error={}", restaurantId, tableNumber, e.getMessage());
            return null;
        }
    }

    public Map<String, Object> getKitchenStats(Long restaurantId, long start, long end) {
        List<KitchenTicket> tickets = ticketRepository.findByRestaurantIdAndCreatedAtBetween(restaurantId, start, end);

        long totalTickets = tickets.size();
        long openTickets = tickets.stream().filter(t -> t.getStatus() == TicketStatus.OPEN).count();
        long inProgressTickets = tickets.stream().filter(t -> t.getStatus() == TicketStatus.IN_PROGRESS).count();
        long readyTickets = tickets.stream().filter(t -> t.getStatus() == TicketStatus.READY).count();
        long pickedUpTickets = tickets.stream().filter(t -> t.getStatus() == TicketStatus.PICKED_UP).count();
        long completedTickets = readyTickets + pickedUpTickets;

        double avgPrepTimeMs = tickets.stream()
            .filter(t -> (t.getStatus() == TicketStatus.READY || t.getStatus() == TicketStatus.PICKED_UP) && t.getReadyAt() > t.getCreatedAt())
            .mapToLong(t -> t.getReadyAt() - t.getCreatedAt())
            .average()
            .orElse(0.0);

        Map<String, Object> stats = new HashMap<>();
        stats.put("restaurantId", restaurantId);
        stats.put("startTime", start);
        stats.put("endTime", end);
        stats.put("totalTickets", totalTickets);
        stats.put("openTickets", openTickets);
        stats.put("inProgressTickets", inProgressTickets);
        stats.put("readyTickets", readyTickets);
        stats.put("pickedUpTickets", pickedUpTickets);
        stats.put("completedTickets", completedTickets);
        stats.put("averagePrepTimeMs", Math.round(avgPrepTimeMs));

        Map<String, Map<String, Object>> stationStats = new HashMap<>();
        for (KitchenStation station : KitchenStation.values()) {
            List<KitchenTicket> stationTickets = tickets.stream()
                .filter(t -> t.getStation() == station)
                .toList();

            long sTotal = stationTickets.size();
            long sCompleted = stationTickets.stream()
                .filter(t -> t.getStatus() == TicketStatus.READY || t.getStatus() == TicketStatus.PICKED_UP)
                .count();

            double sAvgPrepTime = stationTickets.stream()
                .filter(t -> (t.getStatus() == TicketStatus.READY || t.getStatus() == TicketStatus.PICKED_UP) && t.getReadyAt() > t.getCreatedAt())
                .mapToLong(t -> t.getReadyAt() - t.getCreatedAt())
                .average()
                .orElse(0.0);

            Map<String, Object> sMap = new HashMap<>();
            sMap.put("total", sTotal);
            sMap.put("completed", sCompleted);
            sMap.put("averagePrepTimeMs", Math.round(sAvgPrepTime));
            stationStats.put(station.name(), sMap);
        }
        stats.put("stationStats", stationStats);

        return stats;
    }
}
