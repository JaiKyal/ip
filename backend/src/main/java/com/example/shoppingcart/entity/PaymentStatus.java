package com.example.shoppingcart.entity;

/** Simulated payment state. Matches the check constraint on orders.payment_status. */
public enum PaymentStatus {
    PENDING,
    PAID,
    FAILED,
    REFUNDED
}
