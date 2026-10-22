package com.example.shoppingcart.dto;

import com.example.shoppingcart.entity.Cart;
import java.math.BigDecimal;
import java.util.List;

public record CartResponse(
        List<CartItemResponse> items,
        int totalQuantity,
        BigDecimal subtotal,
        boolean hasUnavailableItems) {

    public static CartResponse from(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream().map(CartItemResponse::from).toList();
        BigDecimal subtotal = items.stream()
                .map(CartItemResponse::lineTotal)
                .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
        return new CartResponse(
                items,
                items.stream().mapToInt(CartItemResponse::quantity).sum(),
                subtotal,
                items.stream().anyMatch(i -> !i.available()));
    }
}
