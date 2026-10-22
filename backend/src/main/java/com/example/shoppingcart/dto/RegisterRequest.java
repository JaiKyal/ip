package com.example.shoppingcart.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * What a visitor sends to create an account. There is deliberately no role field: new accounts are always
 * customers, so nobody can register themselves as an admin. (BCrypt only reads the first 72 bytes, hence the limit.)
 */
public record RegisterRequest(
        @NotBlank @Size(max = 120) String fullName,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 72) String password) {
}
