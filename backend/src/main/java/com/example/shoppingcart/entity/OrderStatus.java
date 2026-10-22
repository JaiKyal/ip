package com.example.shoppingcart.entity;

/** Where an order is in its life. Matches the check constraint on orders.status. */
public enum OrderStatus {
    PENDING,
    CONFIRMED,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED
}
