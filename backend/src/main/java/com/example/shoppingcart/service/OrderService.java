package com.example.shoppingcart.service;

import com.example.shoppingcart.dto.OrderResponse;
import com.example.shoppingcart.dto.OrderSummaryResponse;
import com.example.shoppingcart.dto.PageResponse;
import com.example.shoppingcart.entity.Order;
import com.example.shoppingcart.exception.ResourceNotFoundException;
import com.example.shoppingcart.repository.OrderRepository;
import com.example.shoppingcart.repository.PaymentRepository;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** A customer's own orders. Every lookup includes the user id, so nobody can open someone else's order. */
@Service
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orders;
    private final PaymentRepository payments;

    public OrderService(OrderRepository orders, PaymentRepository payments) {
        this.orders = orders;
        this.payments = payments;
    }

    public PageResponse<OrderSummaryResponse> myOrders(Long userId, int page, int size) {
        PageRequest paging = PageRequest.of(page, size, Sort.by(Sort.Order.desc("orderDate"), Sort.Order.desc("id")));
        return PageResponse.from(orders.findByUserId(userId, paging).map(OrderSummaryResponse::from));
    }

    /** Another customer's order id gets the same 404 as an id that does not exist, so ids cannot be probed. */
    public OrderResponse getOwnedOrder(Long userId, Long orderId) {
        Order order = orders.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order " + orderId + " not found"));
        return OrderResponse.from(order, payments.findFirstByOrderIdOrderByIdDesc(order.getId()).orElse(null));
    }

    public Optional<OrderResponse> findByIdempotencyKey(Long userId, String idempotencyKey) {
        return orders.findByUserIdAndIdempotencyKey(userId, idempotencyKey)
                .map(order -> OrderResponse.from(order, payments.findFirstByOrderIdOrderByIdDesc(order.getId()).orElse(null)));
    }
}
