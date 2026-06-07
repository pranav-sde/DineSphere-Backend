package com.festora.orderservice.controller;

import com.festora.orderservice.model.TableRequest;
import com.festora.orderservice.service.TableRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/order/table-request")
@RequiredArgsConstructor
public class TableRequestController {
    private final TableRequestService tableRequestService;

    @PostMapping("/create")
    public ResponseEntity<?> createRequest(
            @RequestHeader("X-Restaurant-Id") Long restaurantId,
            @RequestHeader("X-Table-No") Integer tableNumber,
            @RequestHeader("X-User-Id") String userId,
            @RequestParam String type
    ) {
        try {
            TableRequest request = tableRequestService.createRequest(restaurantId, tableNumber, userId, type);
            return ResponseEntity.ok(request);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/owner/pending")
    public ResponseEntity<List<TableRequest>> getPendingRequests(
            @RequestHeader("X-Restaurant-Id") Long restaurantId
    ) {
        return ResponseEntity.ok(tableRequestService.getPendingRequests(restaurantId));
    }

    @PostMapping("/owner/{id}/resolve")
    public ResponseEntity<TableRequest> resolveRequest(@PathVariable String id) {
        return ResponseEntity.ok(tableRequestService.resolveRequest(id));
    }
}
