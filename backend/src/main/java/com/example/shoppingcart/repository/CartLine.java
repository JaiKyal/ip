package com.example.shoppingcart.repository;

/** A cart row reduced to what checkout needs: which product and how many. Read without loading products. */
public record CartLine(Long productId, int quantity) {
}
