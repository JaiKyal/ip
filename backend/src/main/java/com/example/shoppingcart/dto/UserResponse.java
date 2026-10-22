package com.example.shoppingcart.dto;

import com.example.shoppingcart.entity.Role;
import com.example.shoppingcart.entity.User;

/** A user as the API shows them. The password hash is never part of any response. */
public record UserResponse(Long id, String fullName, String email, Role role) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getRole());
    }
}
