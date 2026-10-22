package com.example.shoppingcart.dto;

import com.example.shoppingcart.entity.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Everything the browser sends to place an order. Note what is missing: no prices, no totals and no
 * items. The server reads the cart and the product prices itself, so none of that can be faked.
 */
public record CheckoutRequest(
        @NotNull @Valid ShippingDetails shipping,
        @NotNull PaymentMethod paymentMethod) {
}
