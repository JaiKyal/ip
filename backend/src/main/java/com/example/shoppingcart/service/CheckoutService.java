package com.example.shoppingcart.service;

import com.example.shoppingcart.dto.CheckoutRequest;
import com.example.shoppingcart.dto.OrderResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Entry point for checkout. It is deliberately NOT transactional itself: the real work runs in
 * {@link OrderPlacementService} in its own transaction. If that transaction loses a race on the
 * idempotency key (the database's unique index rejects the second insert), this class catches it and
 * returns the order that won, so a double submit still produces exactly one order.
 */
@Service
public class CheckoutService {

    public record Result(OrderResponse order, boolean created) {
    }

    private final OrderPlacementService placement;
    private final OrderService orderService;

    public CheckoutService(OrderPlacementService placement, OrderService orderService) {
        this.placement = placement;
        this.orderService = orderService;
    }

    public Result checkout(Long userId, CheckoutRequest request, String idempotencyKey) {
        try {
            OrderPlacementService.Placement placed = placement.place(userId, request, idempotencyKey);
            return new Result(placed.order(), placed.created());
        } catch (DataIntegrityViolationException e) {
            return orderService.findByIdempotencyKey(userId, idempotencyKey)
                    .map(order -> new Result(order, false))
                    .orElseThrow(() -> e);
        }
    }
}
