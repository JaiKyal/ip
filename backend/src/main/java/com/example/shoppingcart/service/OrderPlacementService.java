package com.example.shoppingcart.service;

import com.example.shoppingcart.dto.CheckoutRequest;
import com.example.shoppingcart.dto.OrderResponse;
import com.example.shoppingcart.dto.ShippingDetails;
import com.example.shoppingcart.entity.Cart;
import com.example.shoppingcart.entity.Order;
import com.example.shoppingcart.entity.OrderStatus;
import com.example.shoppingcart.entity.Payment;
import com.example.shoppingcart.entity.PaymentMethod;
import com.example.shoppingcart.entity.PaymentStatus;
import com.example.shoppingcart.entity.Product;
import com.example.shoppingcart.exception.BadRequestException;
import com.example.shoppingcart.exception.ConflictException;
import com.example.shoppingcart.exception.PaymentFailedException;
import com.example.shoppingcart.repository.CartItemRepository;
import com.example.shoppingcart.repository.CartLine;
import com.example.shoppingcart.repository.CartRepository;
import com.example.shoppingcart.repository.OrderRepository;
import com.example.shoppingcart.repository.PaymentRepository;
import com.example.shoppingcart.repository.ProductRepository;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The heart of the shop: turns a cart into an order in ONE database transaction. Either every step happens
 * (order, order lines, payment record, stock reduction, cart emptied) or none of them does.
 *
 * <ol>
 *   <li>Lock the user's cart row, so this user's checkouts run one at a time.</li>
 *   <li>If this idempotency key already produced an order, return that order (a double submit).</li>
 *   <li>Lock the product rows in id order (SELECT ... FOR UPDATE), then check stock on the locked rows.</li>
 *   <li>Take prices from the database, never from the browser, and copy name and price into the order lines.</li>
 *   <li>Charge the simulated gateway. A decline throws, which rolls everything back.</li>
 *   <li>Save the order and payment, and empty the cart. Stock was already reduced on the locked rows.</li>
 * </ol>
 */
@Service
public class OrderPlacementService {

    private static final String ORDER_NUMBER_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final DateTimeFormatter ORDER_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final CartRepository carts;
    private final CartItemRepository cartItems;
    private final ProductRepository products;
    private final OrderRepository orders;
    private final PaymentRepository payments;
    private final PaymentGateway gateway;
    private final SecureRandom random = new SecureRandom();

    public OrderPlacementService(CartRepository carts, CartItemRepository cartItems, ProductRepository products,
                                 OrderRepository orders, PaymentRepository payments, PaymentGateway gateway) {
        this.carts = carts;
        this.cartItems = cartItems;
        this.products = products;
        this.orders = orders;
        this.payments = payments;
        this.gateway = gateway;
    }

    /** The order as the API shows it, and whether it was created now (true) or found from an earlier identical request. */
    public record Placement(OrderResponse order, boolean created) {
    }

    @Transactional
    public Placement place(Long userId, CheckoutRequest request, String idempotencyKey) {
        carts.insertIfAbsent(userId);
        Cart cart = carts.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new IllegalStateException("Cart missing right after creation"));

        Optional<Order> earlier = orders.findByUserIdAndIdempotencyKey(userId, idempotencyKey);
        if (earlier.isPresent()) {
            Order order = earlier.get();
            Payment payment = payments.findFirstByOrderIdOrderByIdDesc(order.getId()).orElse(null);
            return new Placement(OrderResponse.from(order, payment), false);
        }

        List<CartLine> lines = cartItems.findLines(cart.getId());
        if (lines.isEmpty()) {
            throw new BadRequestException("Your cart is empty");
        }

        List<Long> productIds = lines.stream().map(CartLine::productId).sorted().toList();
        Map<Long, Product> locked = products.lockAllByIdOrderById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        List<String> problems = new ArrayList<>();
        for (CartLine line : lines) {
            Product product = locked.get(line.productId());
            if (product == null || !product.isActive()) {
                problems.add((product == null ? "A product" : product.getName()) + " is no longer available");
            } else if (product.getStockQuantity() < line.quantity()) {
                problems.add(product.getStockQuantity() == 0
                        ? product.getName() + " is out of stock"
                        : "Only " + product.getStockQuantity() + " left in stock for " + product.getName());
            }
        }
        if (!problems.isEmpty()) {
            throw new ConflictException(String.join("; ", problems));
        }

        ShippingDetails shipping = request.shipping();
        Order order = new Order(newOrderNumber(), userId, request.paymentMethod(), idempotencyKey,
                shipping.name().trim(), shipping.phone().trim(), shipping.address().trim(), shipping.city().trim(),
                shipping.pincode().trim());
        for (CartLine line : lines) {
            Product product = locked.get(line.productId());
            order.addLine(product, line.quantity());
            product.decreaseStock(line.quantity());
        }

        // Cash on delivery is paid later, so it stays PENDING; UPI and card go through the simulated gateway now.
        String paymentReference = null;
        if (request.paymentMethod() != PaymentMethod.COD) {
            PaymentGateway.PaymentResult result = gateway.charge(request.paymentMethod(), order.getTotalAmount());
            if (!result.approved()) {
                // Nothing has been saved yet and the stock changes are only in memory, so a decline leaves no trace.
                throw new PaymentFailedException("Your payment was declined. You have not been charged.");
            }
            paymentReference = result.reference();
            order.markPaid();
            order.setStatus(OrderStatus.CONFIRMED);
        }

        orders.saveAndFlush(order);
        Payment payment = null;
        if (paymentReference != null) {
            payment = payments.save(new Payment(order.getId(), request.paymentMethod(), PaymentStatus.PAID,
                    order.getTotalAmount(), paymentReference));
        }

        OrderResponse response = OrderResponse.from(order, payment);
        cartItems.deleteAllByCartId(cart.getId());
        return new Placement(response, true);
    }

    private String newOrderNumber() {
        StringBuilder suffix = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            suffix.append(ORDER_NUMBER_ALPHABET.charAt(random.nextInt(ORDER_NUMBER_ALPHABET.length())));
        }
        return "ORD-" + LocalDate.now(ZoneOffset.UTC).format(ORDER_DATE) + "-" + suffix;
    }
}
