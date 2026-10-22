package com.example.shoppingcart.dto;

/** Returned by register and login. Send the token back as "Authorization: Bearer <token>". */
public record AuthResponse(String token, String tokenType, long expiresInSeconds, UserResponse user) {
}
