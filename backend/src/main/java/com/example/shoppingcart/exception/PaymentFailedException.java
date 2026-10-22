package com.example.shoppingcart.exception;

/** Thrown when the (simulated) payment is declined. Becomes HTTP 402 and the whole checkout is rolled back. */
public class PaymentFailedException extends RuntimeException {

    public PaymentFailedException(String message) {
        super(message);
    }
}
