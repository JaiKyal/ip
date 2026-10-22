package com.example.shoppingcart.dto;

import com.example.shoppingcart.entity.Product;
import java.math.BigDecimal;

/** What the public API returns for a product. A DTO, so entities never leave the service layer. */
public record ProductResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        int stockQuantity,
        boolean inStock,
        String imageUrl,
        CategoryResponse category) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getStockQuantity() > 0,
                product.getImageUrl(),
                CategoryResponse.from(product.getCategory()));
    }
}
