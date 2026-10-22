package com.example.shoppingcart.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Where to deliver the order (Indian phone number and 6-digit PIN code). */
public record ShippingDetails(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Pattern(regexp = "^\\+?[0-9]{10,13}$", message = "must be a valid phone number (10 to 13 digits)") String phone,
        @NotBlank @Size(min = 5, max = 300) String address,
        @NotBlank @Size(min = 2, max = 80) String city,
        @NotBlank @Pattern(regexp = "^[0-9]{6}$", message = "must be a 6-digit PIN code") String pincode) {
}
