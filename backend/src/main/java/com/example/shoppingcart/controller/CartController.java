package com.example.shoppingcart.controller;

import com.example.shoppingcart.dto.AddCartItemRequest;
import com.example.shoppingcart.dto.CartResponse;
import com.example.shoppingcart.dto.UpdateCartItemRequest;
import com.example.shoppingcart.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** The logged-in user's cart. The user always comes from the verified token, never from the request. */
@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public CartResponse get(@AuthenticationPrincipal Jwt jwt) {
        return cartService.getCart(userId(jwt));
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public CartResponse add(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AddCartItemRequest request) {
        return cartService.addItem(userId(jwt), request);
    }

    @PatchMapping("/items/{itemId}")
    public CartResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable Long itemId,
                               @Valid @RequestBody UpdateCartItemRequest request) {
        return cartService.updateQuantity(userId(jwt), itemId, request);
    }

    @DeleteMapping("/items/{itemId}")
    public CartResponse remove(@AuthenticationPrincipal Jwt jwt, @PathVariable Long itemId) {
        return cartService.removeItem(userId(jwt), itemId);
    }

    private static Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
