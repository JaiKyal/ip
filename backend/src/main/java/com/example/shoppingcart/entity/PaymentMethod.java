package com.example.shoppingcart.entity;

/** How the customer chose to pay. All of these are simulated; nothing real is charged. */
public enum PaymentMethod {
    UPI,
    CARD,
    /** Cash on delivery: the order is paid when it is delivered. */
    COD
}
