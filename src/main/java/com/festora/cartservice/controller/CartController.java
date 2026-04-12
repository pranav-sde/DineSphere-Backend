package com.festora.cartservice.controller;

import com.festora.cartservice.dto.AddToCartRequest;
import com.festora.cartservice.dto.CheckoutRequest;
import com.festora.cartservice.dto.UpdateCartItemRequest;
import com.festora.cartservice.model.Cart;
import com.festora.cartservice.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Allow all origins for development
public class CartController {

    private final CartService cartService;

    @GetMapping("/health")
    public String health() {
        return "CART OK";
    }


    @PostMapping("/items")
    public Cart addItem(@RequestBody AddToCartRequest req,
                        @RequestHeader(value = "X-Restaurant-Id", required = false) Long restaurantId,
                        @RequestHeader(value = "X-Table-No", required = false) Integer tableNumber,
                        @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        // Debug log for mobile/prod issues
        System.out.println("DEBUG: Cart.addItem called. RID=" + restaurantId + ", TNo=" + tableNumber + ", UID=" + userId);

        req.setRestaurantId(restaurantId);
        req.setTableNumber(tableNumber != null ? tableNumber : 0); // Default to 0 if missing
        req.setSessionId(userId);
        return cartService.addItem(req);
    }

    @GetMapping()
    public Cart viewCart(
            @RequestHeader("X-Restaurant-Id") Long restaurantId,
            @RequestHeader("X-Table-No") Integer tableNo,
            @RequestHeader("X-User-Id") String userId) {
        return cartService.getCart(restaurantId, tableNo, userId);
    }

//    @PutMapping("/items/{cartItemId}")
//    public Cart updateItemQuantity(
//            @PathVariable String cartItemId,
//            @RequestBody UpdateCartItemRequest req
//    ) {
//        return cartService.updateItemQuantity(cartItemId, req);
//    }

    @DeleteMapping
    public void clearCart(@RequestBody CheckoutRequest request) {
        cartService.clearCart(request);
    }

    @DeleteMapping("/items/{cartItemId}")
    public Cart removeItem(
            @PathVariable String cartItemId,
            @RequestHeader(value = "X-Restaurant-Id", required = false) Long restaurantId,
            @RequestHeader(value = "X-Table-No", required = false) Integer tableNo,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        CheckoutRequest req = CheckoutRequest.builder()
                .restaurantId(restaurantId)
                .tableNumber(tableNo != null ? tableNo : 0)
                .userId(userId)
                .build();
        return cartService.removeItem(req, cartItemId);
    }


    @PutMapping("/items/{cartItemId}")
    public Cart updateItemQuantity(
            @PathVariable String cartItemId,
            @RequestBody UpdateCartItemRequest req,
            @RequestHeader("X-Restaurant-Id") Long restaurantId,
            @RequestHeader("X-Table-No") Integer tableNo,
            @RequestHeader("X-User-Id") String userId
    ) {
        req.setRestaurantId(restaurantId);
        req.setUserId(userId);
        req.setTableNumber(tableNo);
        return cartService.updateItemQuantity(cartItemId, req);
    }

    @PostMapping("/checkout")
    public Object checkout(
            @RequestBody CheckoutRequest request,
            @RequestHeader("X-Restaurant-Id") Long restaurantId,
            @RequestHeader("X-Table-No") Integer tableNumber,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-Device-Id", required = false) String deviceId
    ) {
        // Force the secure values BEFORE checking out!
        request.setRestaurantId(restaurantId);
        request.setUserId(userId);
        request.setDeviceId(deviceId);
        request.setTableNumber(tableNumber);

        return cartService.checkout(request);
    }

}

