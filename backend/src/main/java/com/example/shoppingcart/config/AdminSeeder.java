package com.example.shoppingcart.config;

import com.example.shoppingcart.entity.Role;
import com.example.shoppingcart.entity.User;
import com.example.shoppingcart.repository.UserRepository;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the first admin at startup from ADMIN_EMAIL and ADMIN_PASSWORD, but only if no admin exists yet.
 * The password is hashed here, so no password or hash is ever committed to the repository, and there is no
 * public endpoint that can create an admin.
 */
@Component
public class AdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);
    private static final int MIN_PASSWORD_LENGTH = 8;

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AdminProperties properties;

    public AdminSeeder(UserRepository users, PasswordEncoder passwordEncoder, AdminProperties properties) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (users.existsByRole(Role.ADMIN)) {
            log.info("An admin account already exists; nothing to seed");
            return;
        }
        String email = properties.email() == null ? "" : properties.email().trim().toLowerCase(Locale.ROOT);
        String password = properties.password() == null ? "" : properties.password();
        if (email.isEmpty() || password.isEmpty()) {
            log.warn("No admin account exists and ADMIN_EMAIL / ADMIN_PASSWORD are not set; admin features are unavailable");
            return;
        }
        if (!email.contains("@") || email.startsWith("<")) {
            log.warn("ADMIN_EMAIL does not look like an email address; admin account not created");
            return;
        }
        if (password.length() < MIN_PASSWORD_LENGTH || password.length() > 72 || password.startsWith("<")) {
            log.warn("ADMIN_PASSWORD must be 8 to 72 characters; admin account not created");
            return;
        }
        users.save(new User("Administrator", email, passwordEncoder.encode(password), Role.ADMIN));
        log.info("Created the initial admin account for {}", email);
    }
}
