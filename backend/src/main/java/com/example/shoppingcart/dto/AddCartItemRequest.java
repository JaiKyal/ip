package com.example.shoppingcart.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Add a product to the cart. There is no price field: prices always come from the database. */
public record AddCartItemRequest(
        @NotNull Long productId,
        @Min(1) @Max(99) int quantity) {
}
