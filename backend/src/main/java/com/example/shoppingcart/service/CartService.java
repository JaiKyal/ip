package com.example.shoppingcart.service;

import com.example.shoppingcart.dto.AddCartItemRequest;
import com.example.shoppingcart.dto.CartResponse;
import com.example.shoppingcart.dto.UpdateCartItemRequest;
import com.example.shoppingcart.entity.Cart;
import com.example.shoppingcart.entity.CartItem;
import com.example.shoppingcart.entity.Product;
import com.example.shoppingcart.exception.ConflictException;
import com.example.shoppingcart.exception.ResourceNotFoundException;
import com.example.shoppingcart.repository.CartRepository;
import com.example.shoppingcart.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The customer's database-backed cart. Every change locks the user's cart row first, so two requests from the
 * same user (a double click, two tabs) are handled one after the other. Stock is checked here on the server;
 * the browser's own checks are only a convenience.
 */
@Service
@Transactional
public class CartService {

    private final CartRepository carts;
    private final ProductRepository products;

    public CartService(CartRepository carts, ProductRepository products) {
        this.carts = carts;
        this.products = products;
    }

    public CartResponse getCart(Long userId) {
        return CartResponse.from(lockedCart(userId));
    }

    public CartResponse addItem(Long userId, AddCartItemRequest request) {
        Cart cart = lockedCart(userId);
        Product product = products.findByIdAndActiveTrue(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product " + request.productId() + " not found"));

        CartItem existing = cart.findItemByProductId(product.getId()).orElse(null);
        int wanted = (existing == null ? 0 : existing.getQuantity()) + request.quantity();
        requireStock(product, wanted);

        if (existing == null) {
            cart.addItem(product, wanted);
        } else {
            existing.setQuantity(wanted);
        }
        carts.flush();
        return CartResponse.from(cart);
    }

    public CartResponse updateQuantity(Long userId, Long itemId, UpdateCartItemRequest request) {
        Cart cart = lockedCart(userId);
        CartItem item = cart.findItemById(itemId).orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));
        requireStock(item.getProduct(), request.quantity());
        item.setQuantity(request.quantity());
        carts.flush();
        return CartResponse.from(cart);
    }

    public CartResponse removeItem(Long userId, Long itemId) {
        Cart cart = lockedCart(userId);
        CartItem item = cart.findItemById(itemId).orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));
        cart.removeItem(item);
        carts.flush();
        return CartResponse.from(cart);
    }

    /** Finds (or creates) the user's cart and locks it. Items are looked up in this cart only, never by id alone. */
    private Cart lockedCart(Long userId) {
        carts.insertIfAbsent(userId);
        return carts.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new IllegalStateException("Cart missing right after creation"));
    }

    private static void requireStock(Product product, int wanted) {
        if (product.getStockQuantity() <= 0) {
            throw new ConflictException(product.getName() + " is out of stock");
        }
        if (wanted > product.getStockQuantity()) {
            throw new ConflictException("Only " + product.getStockQuantity() + " left in stock for " + product.getName());
        }
    }
}
