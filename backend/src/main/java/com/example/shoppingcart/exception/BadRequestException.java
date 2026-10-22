package com.example.shoppingcart.exception;

/** Thrown when the client sent something invalid. Becomes HTTP 400. */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
