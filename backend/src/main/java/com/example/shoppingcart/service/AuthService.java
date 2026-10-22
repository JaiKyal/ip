package com.example.shoppingcart.service;

import com.example.shoppingcart.dto.AuthResponse;
import com.example.shoppingcart.dto.LoginRequest;
import com.example.shoppingcart.dto.RegisterRequest;
import com.example.shoppingcart.dto.UserResponse;
import com.example.shoppingcart.entity.Role;
import com.example.shoppingcart.entity.User;
import com.example.shoppingcart.exception.AuthenticationFailedException;
import com.example.shoppingcart.exception.ConflictException;
import com.example.shoppingcart.exception.ResourceNotFoundException;
import com.example.shoppingcart.repository.UserRepository;
import com.example.shoppingcart.security.JwtService;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    /** Checked when the email is unknown, so unknown and wrong-password logins take about the same time. */
    private final String decoyHash;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.decoyHash = passwordEncoder.encode("not-a-real-password");
    }

    /** New accounts are always CUSTOMER. The role is never taken from the request. */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalize(request.email());
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("An account with this email already exists");
        }
        User user = new User(request.fullName().trim(), email, passwordEncoder.encode(request.password()), Role.CUSTOMER);
        try {
            users.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // Two people registering the same email at the same moment: the database unique index decides.
            throw new ConflictException("An account with this email already exists");
        }
        return respond(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = users.findByEmailIgnoreCase(normalize(request.email())).orElse(null);
        if (user == null) {
            passwordEncoder.matches(request.password(), decoyHash);
            throw new AuthenticationFailedException();
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash()) || !user.isEnabled()) {
            throw new AuthenticationFailedException();
        }
        return respond(user);
    }

    @Transactional(readOnly = true)
    public UserResponse currentUser(Long userId) {
        return users.findById(userId)
                .filter(User::isEnabled)
                .map(UserResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private AuthResponse respond(User user) {
        return new AuthResponse(jwtService.issueFor(user), "Bearer", jwtService.lifetimeSeconds(), UserResponse.from(user));
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
