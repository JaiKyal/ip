package com.example.shoppingcart.exception;

/** Thrown when a requested record does not exist (or is hidden). Becomes HTTP 404. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
