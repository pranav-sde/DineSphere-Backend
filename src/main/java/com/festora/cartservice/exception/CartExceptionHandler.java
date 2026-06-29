package com.festora.cartservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.NoSuchElementException;

@RestControllerAdvice
public class CartExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(getFriendlyMessage(e.getMessage()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<String> handleNotFound(NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(getFriendlyMessage(e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleIllegalState(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(getFriendlyMessage(e.getMessage()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleRuntime(RuntimeException e) {
        // If it's a wrapped message from CartService checkout
        String msg = e.getMessage();
        if (msg != null && msg.contains(": ")) {
            msg = msg.substring(msg.indexOf(": ") + 2);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(getFriendlyMessage(msg));
    }

    private String getFriendlyMessage(String technicalCode) {
        if (technicalCode == null) return "An unexpected error occurred. Please try again.";

        return switch (technicalCode) {
            case "INSUFFICIENT_STOCK" -> "Some items in your cart are no longer available in the requested quantity.";
            case "ITEM_DISABLED" -> "One or more items in your cart are currently unavailable.";
            case "ITEM_NOT_FOUND" -> "One or more items in your cart could not be found in our inventory.";
            case "OUT_OF_STOCK" -> "Some items are currently out of stock.";
            case "INVALID_MENU_PRICE" -> "There was a price mismatch for one of your items. Please refresh your cart.";
            case "Cart empty" -> "Your cart is empty. Please add some items before checking out.";
            case "ORDER_NOT_EDITABLE" -> "This order can no longer be modified as it is already being prepared.";
            case "EMPTY_ORDER" -> "Your order must contain at least one item.";
            case "SUBSCRIPTION_EXPIRED" -> "This restaurant is not accepting orders right now.";
            default -> technicalCode.contains(" ") ? technicalCode : "Order creation failed: " + technicalCode;
        };
    }
}
