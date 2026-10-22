package com.example.shoppingcart.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Credentials for the first admin account, read from ADMIN_EMAIL and ADMIN_PASSWORD. Both optional. */
@ConfigurationProperties(prefix = "app.admin")
public record AdminProperties(String email, String password) {
}
