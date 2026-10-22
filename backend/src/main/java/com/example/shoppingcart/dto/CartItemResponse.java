package com.example.shoppingcart.dto;

import com.example.shoppingcart.entity.CartItem;
import com.example.shoppingcart.entity.Product;
import java.math.BigDecimal;

public record CartItemResponse(
        Long id,
        Long productId,
        String name,
        String imageUrl,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineTotal,
        int stockQuantity,
        /** False when the product was hidden or its stock fell below the quantity in the cart. */
        boolean available) {

    public static CartItemResponse from(CartItem item) {
        Product product = item.getProduct();
        return new CartItemResponse(
                item.getId(),
                product.getId(),
                product.getName(),
                product.getImageUrl(),
                product.getPrice(),
                item.getQuantity(),
                product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())),
                product.getStockQuantity(),
                product.isActive() && product.getStockQuantity() >= item.getQuantity());
    }
}
