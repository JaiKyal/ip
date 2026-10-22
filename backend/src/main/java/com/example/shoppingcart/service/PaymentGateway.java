package com.example.shoppingcart.service;

import com.example.shoppingcart.entity.PaymentMethod;
import java.math.BigDecimal;

/**
 * The seam where a real payment provider would plug in. This project only has a simulated implementation,
 * and no card numbers or other payment credentials ever reach the server.
 */
public interface PaymentGateway {

    PaymentResult charge(PaymentMethod method, BigDecimal amount);

    record PaymentResult(boolean approved, String reference) {
    }
}
