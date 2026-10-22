package com.example.shoppingcart.exception;

import java.time.Instant;

/** The one JSON shape every error response uses. */
public record ApiError(Instant timestamp, int status, String error, String message, String path) {
}
