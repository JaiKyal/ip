package com.example.shoppingcart.exception;

/** Thrown when login fails. The message is always the same so it does not reveal which emails exist. Becomes HTTP 401. */
public class AuthenticationFailedException extends RuntimeException {

    public AuthenticationFailedException() {
        super("Invalid email or password");
    }
}
