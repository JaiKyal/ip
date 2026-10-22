package com.example.shoppingcart.support;

import com.example.shoppingcart.entity.PaymentMethod;
import com.example.shoppingcart.service.PaymentGateway;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Test-only payment gateway that replaces the simulated one in tests. It approves by default and can be told
 * to decline, which lets tests prove that a failed payment leaves no order, no payment and no stock change.
 */
@Component
@Primary
public class ControllablePaymentGateway implements PaymentGateway {

    private volatile boolean decline;

    public void setDecline(boolean decline) {
        this.decline = decline;
    }

    @Override
    public PaymentResult charge(PaymentMethod method, BigDecimal amount) {
        if (decline) {
            return new PaymentResult(false, null);
        }
        return new PaymentResult(true, "SIM-TEST-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }
}
