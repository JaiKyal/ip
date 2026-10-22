package com.example.shoppingcart.exception;

/** Thrown when the request clashes with existing data, such as an email that is already registered. Becomes HTTP 409. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
