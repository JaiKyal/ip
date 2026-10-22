package com.example.shoppingcart.controller;

import com.example.shoppingcart.dto.OrderResponse;
import com.example.shoppingcart.dto.OrderSummaryResponse;
import com.example.shoppingcart.dto.PageResponse;
import com.example.shoppingcart.service.OrderService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** A customer's own order history. */
@RestController
@RequestMapping("/api/orders")
@Validated
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/my-orders")
    public PageResponse<OrderSummaryResponse> myOrders(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        return orderService.myOrders(Long.valueOf(jwt.getSubject()), page, size);
    }

    @GetMapping("/{orderId}")
    public OrderResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long orderId) {
        return orderService.getOwnedOrder(Long.valueOf(jwt.getSubject()), orderId);
    }
}
