package com.example.shoppingcart.controller;

import com.example.shoppingcart.dto.CheckoutRequest;
import com.example.shoppingcart.dto.OrderResponse;
import com.example.shoppingcart.service.CheckoutService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/checkout")
@Validated
public class CheckoutController {

    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    /**
     * Places the order for the current cart. The browser sends a fresh random Idempotency-Key for each checkout
     * attempt: sending the same key again (double click, retry after a network error) returns the same order
     * with 200 instead of creating a second one. A new order is 201.
     */
    @PostMapping
    public ResponseEntity<OrderResponse> checkout(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader("Idempotency-Key")
            @Pattern(regexp = "^[A-Za-z0-9_-]{8,64}$", message = "must be 8 to 64 letters, digits, dashes or underscores")
            String idempotencyKey,
            @Valid @RequestBody CheckoutRequest request) {
        CheckoutService.Result result = checkoutService.checkout(Long.valueOf(jwt.getSubject()), request, idempotencyKey);
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK).body(result.order());
    }
}
