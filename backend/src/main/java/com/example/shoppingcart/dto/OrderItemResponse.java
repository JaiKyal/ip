package com.example.shoppingcart.dto;

import com.example.shoppingcart.entity.OrderItem;
import java.math.BigDecimal;

/** A line of an order, using the name and price as they were when the order was placed. */
public record OrderItemResponse(Long productId, String name, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {

    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getProductId(), item.getProductNameSnapshot(), item.getQuantity(), item.getUnitPrice(), item.getLineTotal());
    }
}
