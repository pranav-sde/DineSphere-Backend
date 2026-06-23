package com.festora.barservice.service;

import com.festora.barservice.model.BarInventoryItem;
import com.festora.barservice.model.BarTicket;
import com.festora.barservice.repository.BarInventoryRepository;
import com.festora.barservice.repository.BarTicketRepository;
import com.festora.kitchenservice.enums.TicketStatus;
import com.festora.kitchenservice.model.KitchenTicket;
import com.festora.kitchenservice.model.TicketItem;
import com.festora.kitchenservice.repository.KitchenTicketRepository;
import com.festora.orderservice.enums.OrderStatus;
import com.festora.orderservice.model.Order;
import com.festora.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BarService {

    private final BarInventoryRepository barInventoryRepository;
    private final BarTicketRepository barTicketRepository;
    private final KitchenTicketRepository kitchenTicketRepository;
    private final OrderRepository orderRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // Deduct stock when bar ticket is confirmed
    public void deductBarStock(Long restaurantId, List<TicketItem> drinks) {
        for (TicketItem drink : drinks) {
            barInventoryRepository
                .findByRestaurantIdAndItemName(restaurantId, drink.getName())
                .ifPresent(item -> {
                    double newStock = item.getAvailableStock() - drink.getQuantity();
                    item.setAvailableStock(Math.max(0, newStock));
                    item.setUpdatedAt(System.currentTimeMillis());
                    barInventoryRepository.save(item);

                    // Low stock alert to admin
                    if (item.getAvailableStock() <= item.getLowStockThreshold()) {
                        try {
                            messagingTemplate.convertAndSend(
                                "/topic/admin/" + restaurantId,
                                Map.of(
                                    "type", "BAR_LOW_STOCK",
                                    "item", item.getItemName(),
                                    "remaining", item.getAvailableStock(),
                                    "unit", item.getUnit() != null ? item.getUnit().name() : "UNIT"
                                )
                            );
                        } catch (Exception e) {
                            log.error("Failed to send low stock alert: {}", e.getMessage());
                        }
                    }
                });
        }
    }

    // Bartender marks drink ready
    public void markBarTicketReady(String ticketId) {
        BarTicket ticket = barTicketRepository.findByTicketId(ticketId)
            .orElseThrow(() -> new RuntimeException("Bar ticket not found: " + ticketId));
        ticket.setStatus(TicketStatus.READY);
        ticket.setReadyAt(System.currentTimeMillis());
        ticket.setUpdatedAt(System.currentTimeMillis());
        barTicketRepository.save(ticket);

        // Sync with KitchenTicket if exists
        try {
            Optional<KitchenTicket> ktOpt = kitchenTicketRepository.findByTicketId(ticketId);
            if (ktOpt.isPresent()) {
                KitchenTicket kt = ktOpt.get();
                kt.setStatus(TicketStatus.READY);
                kt.setReadyAt(System.currentTimeMillis());
                kt.setUpdatedAt(System.currentTimeMillis());
                kitchenTicketRepository.save(kt);

                // Check if all tickets for this order are ready
                checkAllTicketsReady(kt.getOrderId(), kt.getRestaurantId());
            }
        } catch (Exception e) {
            log.error("Failed to sync KitchenTicket for bar ticket {}: {}", ticketId, e.getMessage());
        }

        // Notify captain
        try {
            messagingTemplate.convertAndSend(
                "/topic/captain/" + ticket.getRestaurantId(),
                Map.of(
                    "type", "DRINK_READY",
                    "ticketId", ticketId,
                    "tableNumber", ticket.getTableNumber() != null ? ticket.getTableNumber() : 0,
                    "items", ticket.getDrinks().stream().map(TicketItem::getName).collect(Collectors.toList())
                )
            );
        } catch (Exception e) {
            log.error("Failed to notify captain on drink ready: {}", e.getMessage());
        }
    }

    // Owner views all bar stock
    public List<BarInventoryItem> getBarInventory(Long restaurantId) {
        return barInventoryRepository.findByRestaurantId(restaurantId);
    }

    // Owner adds new bar inventory item
    public BarInventoryItem addBarItem(BarInventoryItem item) {
        item.setUpdatedAt(System.currentTimeMillis());
        if (item.getAvailableStock() == 0 && item.getTotalStock() > 0) {
            item.setAvailableStock(item.getTotalStock());
        }
        return barInventoryRepository.save(item);
    }

    // Update bar stock (owner adds new bottles)
    public BarInventoryItem updateStock(Long restaurantId, String itemId, double quantity) {
        BarInventoryItem item = barInventoryRepository.findById(itemId)
            .orElseThrow(() -> new RuntimeException("Inventory item not found: " + itemId));
        item.setTotalStock(item.getTotalStock() + quantity);
        item.setAvailableStock(item.getAvailableStock() + quantity);
        item.setUpdatedAt(System.currentTimeMillis());
        return barInventoryRepository.save(item);
    }

    // Owner: get low stock items
    public List<BarInventoryItem> getLowStockItems(Long restaurantId) {
        return barInventoryRepository.findByRestaurantId(restaurantId).stream()
            .filter(item -> item.getAvailableStock() <= item.getLowStockThreshold())
            .collect(Collectors.toList());
    }

    private void checkAllTicketsReady(String orderId, Long restaurantId) {
        List<KitchenTicket> allTickets = kitchenTicketRepository
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
}
