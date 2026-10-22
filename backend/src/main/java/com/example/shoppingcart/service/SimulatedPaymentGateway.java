package com.example.shoppingcart.service;

import com.example.shoppingcart.entity.PaymentMethod;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** A fake gateway for demos: approves every UPI and card payment and hands back a made-up reference. */
@Component
public class SimulatedPaymentGateway implements PaymentGateway {

    @Override
    public PaymentResult charge(PaymentMethod method, BigDecimal amount) {
        return new PaymentResult(true, "SIM-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
    }
}
