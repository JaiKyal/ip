package com.example.shoppingcart.dto;

import com.example.shoppingcart.entity.Order;
import com.example.shoppingcart.entity.OrderStatus;
import com.example.shoppingcart.entity.Payment;
import com.example.shoppingcart.entity.PaymentMethod;
import com.example.shoppingcart.entity.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id,
        String orderNumber,
        OrderStatus status,
        PaymentStatus paymentStatus,
        PaymentMethod paymentMethod,
        BigDecimal subtotal,
        BigDecimal totalAmount,
        Instant orderDate,
        ShippingDetails shipping,
        List<OrderItemResponse> items,
        PaymentSummary payment) {

    /** The simulated payment record. {@code simulated} is always true: nothing real is ever charged. */
    public record PaymentSummary(String reference, PaymentStatus status, boolean simulated) {
    }

    public static OrderResponse from(Order order, Payment payment) {
        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus(),
                order.getPaymentStatus(),
                order.getPaymentMethod(),
                order.getSubtotal(),
                order.getTotalAmount(),
                order.getOrderDate(),
                new ShippingDetails(order.getShippingName(), order.getShippingPhone(), order.getShippingAddress(),
                        order.getShippingCity(), order.getShippingPincode()),
                order.getItems().stream().map(OrderItemResponse::from).toList(),
                payment == null ? null : new PaymentSummary(payment.getTransactionReference(), payment.getPaymentStatus(), true));
    }
}
