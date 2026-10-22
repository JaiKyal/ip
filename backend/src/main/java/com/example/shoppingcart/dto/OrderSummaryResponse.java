package com.example.shoppingcart.dto;

import com.example.shoppingcart.entity.Order;
import com.example.shoppingcart.entity.OrderItem;
import com.example.shoppingcart.entity.OrderStatus;
import com.example.shoppingcart.entity.PaymentMethod;
import com.example.shoppingcart.entity.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;

/** One row of an order list. */
public record OrderSummaryResponse(
        Long id,
        String orderNumber,
        OrderStatus status,
        PaymentStatus paymentStatus,
        PaymentMethod paymentMethod,
        BigDecimal totalAmount,
        Instant orderDate,
        int itemCount) {

    public static OrderSummaryResponse from(Order order) {
        return new OrderSummaryResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus(),
                order.getPaymentStatus(),
                order.getPaymentMethod(),
                order.getTotalAmount(),
                order.getOrderDate(),
                order.getItems().stream().mapToInt(OrderItem::getQuantity).sum());
    }
}
