package com.festora.cartservice.service;

import com.festora.cartservice.dto.AddToCartRequest;
import com.festora.cartservice.dto.CheckoutRequest;
import com.festora.cartservice.dto.MenuValidationResult;
import com.festora.cartservice.dto.UpdateCartItemRequest;
import com.festora.cartservice.exception.CartExceptionHandler;
import com.festora.cartservice.model.AddonSnapshot;
import com.festora.cartservice.model.Cart;
import com.festora.cartservice.model.CartItem;
import com.festora.cartservice.repository.CartRepository;
import com.festora.orderservice.dto.CreateOrderRequest;
import com.festora.orderservice.model.Order;
import com.festora.orderservice.model.OrderItem;
import com.festora.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private final CartRepository cartRepo;
    private final MenuLocalValidator menuValidator;
    private final OrderService orderService;

    public Cart addItem(AddToCartRequest req) {

        if (ObjectUtils.isEmpty(req) || req.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be >= 1");
        }

        Cart cart = cartRepo.findByRestaurantIdAndTableNumberAndUserId(
                req.getRestaurantId(), req.getTableNumber(), req.getSessionId()
        ).orElseGet(() -> {
            long now = System.currentTimeMillis();
            return Cart.builder()
                    .cartId(UUID.randomUUID().toString())
                    .restaurantId(req.getRestaurantId())
                    .userId(req.getSessionId())
                    .tableNumber(req.getTableNumber())
                    .createdAt(now)
                    .updatedAt(now)
                    .items(new ArrayList<>())
                    .subtotal(0)
                    .build();
        });

        MenuValidationResult menuResult = menuValidator.validate(req.getRestaurantId(), req.getMenuItemId(),
                req.getVariantId(), req.getAddonIds());

        double unitPrice =
                menuResult.getVariantPrice()
                        + menuResult.getAddons()
                        .stream()
                        .mapToDouble(AddonSnapshot::getPrice)
                        .sum();

        String identityKey = buildIdentity(req.getMenuItemId(), req.getVariantId(),
                req.getAddonIds());

        Optional<CartItem> existing =
                cart.getItems().stream()
                        .filter(i -> i.getIdentityKey().equals(identityKey))
                        .findFirst();

        if (existing.isPresent()) {
            CartItem item = existing.get();
            item.setQuantity(item.getQuantity() + req.getQuantity());
            item.setTotalPrice(item.getQuantity() * item.getUnitPrice());
        } else {
            CartItem item = CartItem.builder()
                    .cartItemId(UUID.randomUUID().toString())
                    .identityKey(identityKey)
                    .menuItemId(req.getMenuItemId())
                    .name(menuResult.getItemName())
                    .variant(menuResult.getVariant())
                    .addons(menuResult.getAddons())
                    .imageUrl(req.getImageUrl())
                    .unitPrice(unitPrice)
                    .quantity(req.getQuantity())
                    .totalPrice(unitPrice * req.getQuantity())
                    .build();

            cart.getItems().add(item);
        }

        recalcSubtotal(cart);
        cart.setUpdatedAt(System.currentTimeMillis());

        return cartRepo.save(cart);
    }

    public Cart getCart(Long restaurantId, Integer tableNo, String userId) {
        return cartRepo.findByRestaurantIdAndTableNumberAndUserId(restaurantId, tableNo, userId)
                .orElseGet(() -> emptyCart(restaurantId, tableNo, userId));
    }

    public void clearCart(CheckoutRequest req) {
        cartRepo.deleteByRestaurantIdAndTableNumberAndUserId(req.getRestaurantId(), req.getTableNumber(), req.getUserId());
    }

    private void recalcSubtotal(Cart cart) {
        cart.setSubtotal(
                cart.getItems().stream()
                        .mapToDouble(CartItem::getTotalPrice)
                        .sum()
        );
    }

    private String buildIdentity(
            String menuItemId,
            String variantId,
            List<String> addonIds
    ) {
        List<String> sorted =
                addonIds == null ? new ArrayList<>() : new ArrayList<>(addonIds);

        Collections.sort(sorted);

        return menuItemId
                + "|" + (variantId == null ? "NA" : variantId)
                + "|" + String.join(",", sorted);
    }

    private Cart emptyCart(Long restaurantId, Integer tableNo, String userId) {
        long now = System.currentTimeMillis();

        return Cart.builder()
                .cartId(UUID.randomUUID().toString())
                .restaurantId(restaurantId)
                .tableNumber(tableNo)
                .userId(userId)
                .createdAt(now)
                .updatedAt(now)
                .items(new ArrayList<>())
                .subtotal(0)
                .build();
    }

    public Cart updateItemQuantity(String cartItemId, UpdateCartItemRequest req) {
        if (req == null || req.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be >= 1");
        }

        Cart cart = cartRepo.findByRestaurantIdAndTableNumberAndUserId(
                req.getRestaurantId(), req.getTableNumber(), req.getUserId()
        ).orElseThrow(() -> new NoSuchElementException("Cart not found"));

        CartItem item =
                cart.getItems().stream()
                        .filter(i -> i.getCartItemId().equals(cartItemId))
                        .findFirst()
                        .orElseThrow(() ->
                                new NoSuchElementException("Cart item not found")
                        );

        item.setQuantity(req.getQuantity());
        item.setTotalPrice(item.getUnitPrice() * req.getQuantity());

        recalcSubtotal(cart);
        cart.setUpdatedAt(System.currentTimeMillis());

        return cartRepo.save(cart);
    }

    public Cart removeItem(CheckoutRequest req, String cartItemId) {
        Cart cart = cartRepo.findByRestaurantIdAndTableNumberAndUserId(
                req.getRestaurantId(), req.getTableNumber(), req.getUserId()
        ).orElseThrow(() -> new NoSuchElementException("Cart not found"));

        boolean removed =
                cart.getItems().removeIf(
                        item -> item.getCartItemId().equals(cartItemId)
                );

        if (!removed) {
            throw new NoSuchElementException("Cart item not found");
        }

        recalcSubtotal(cart);
        cart.setUpdatedAt(System.currentTimeMillis());

        if (cart.getItems().isEmpty()) {
            cartRepo.delete(cart);
            return emptyCart(req.getRestaurantId(), req.getTableNumber(), req.getUserId());
        }

        return cartRepo.save(cart);
    }

    public Object checkout(CheckoutRequest req) throws Exception {
        Cart cart = cartRepo.findByRestaurantIdAndTableNumberAndUserId(
                req.getRestaurantId(), req.getTableNumber(), req.getUserId()
        ).orElseThrow(() -> new NoSuchElementException("Cart not found"));

        if (cart.getItems().isEmpty()) {
            throw new IllegalStateException("Cart empty");
        }

        // Step 1: Final menu re-validation
        cart.getItems().forEach(item ->
                menuValidator.validate(
                        req.getRestaurantId(),
                        item.getMenuItemId(),
                        item.getVariant() == null ? null : item.getVariant().getVariantId(),
                        item.getAddons()
                                .stream()
                                .map(AddonSnapshot::getAddonId)
                                .toList()
                )
        );

        // Step 2: Build CreateOrderRequest
        String generatedOrderId = UUID.randomUUID().toString();
        CreateOrderRequest orderRequest = new CreateOrderRequest();
        orderRequest.setOrderId(generatedOrderId);
        orderRequest.setRestaurantId(cart.getRestaurantId());
        orderRequest.setTableNumber(req.getTableNumber());
        orderRequest.setUserId(cart.getUserId());
        orderRequest.setDeviceId(req.getDeviceId());
        orderRequest.setSubtotal(cart.getSubtotal());
        orderRequest.setItems(
                cart.getItems().stream()
                        .map(item -> OrderItem.builder()
                                .menuItemId(item.getMenuItemId())
                                .variantId(
                                        item.getVariant() == null
                                                ? null
                                                : item.getVariant().getVariantId()
                                )
                                .addonIds(
                                        item.getAddons().stream()
                                                .map(AddonSnapshot::getAddonId)
                                                .toList()
                                )
                                .unitPrice(item.getUnitPrice())
                                .quantity(item.getQuantity())
                                .totalPrice(item.getTotalPrice())
                                .build()
                        )
                        .toList()
        );

        Order order = orderService.createOrder(orderRequest);

        // Step 4: Clear cart
        cartRepo.delete(cart);

        // Step 5: Build response
        Map<String, Object> response = new HashMap<>();
        response.put("orderId", order.getOrderId());
        response.put("status", order.getStatus().name());
        response.put("message", "Order placed successfully");
        return response;
    }
}