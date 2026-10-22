package com.example.shoppingcart.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** A placed order. Table name is "orders" because ORDER is an SQL keyword. */
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30, updatable = false)
    private String orderNumber;

    @Column(nullable = false, updatable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus paymentStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, updatable = false)
    private PaymentMethod paymentMethod;

    /** Sent by the browser with each checkout; the same key twice means a double submit, not a second order. */
    @Column(length = 64, updatable = false)
    private String idempotencyKey;

    @Column(nullable = false, updatable = false)
    private Instant orderDate;

    @Column(nullable = false, length = 120, updatable = false)
    private String shippingName;

    @Column(nullable = false, length = 20, updatable = false)
    private String shippingPhone;

    @Column(nullable = false, length = 300, updatable = false)
    private String shippingAddress;

    @Column(nullable = false, length = 80, updatable = false)
    private String shippingCity;

    @Column(nullable = false, length = 10, updatable = false)
    private String shippingPincode;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    @OrderBy("id ASC")
    private List<OrderItem> items = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    protected Order() {
        // required by JPA
    }

    public Order(String orderNumber, Long userId, PaymentMethod paymentMethod, String idempotencyKey,
                 String shippingName, String shippingPhone, String shippingAddress, String shippingCity,
                 String shippingPincode) {
        this.orderNumber = orderNumber;
        this.userId = userId;
        this.paymentMethod = paymentMethod;
        this.idempotencyKey = idempotencyKey;
        this.shippingName = shippingName;
        this.shippingPhone = shippingPhone;
        this.shippingAddress = shippingAddress;
        this.shippingCity = shippingCity;
        this.shippingPincode = shippingPincode;
        this.status = OrderStatus.PENDING;
        this.paymentStatus = PaymentStatus.PENDING;
        this.orderDate = Instant.now();
        this.subtotal = BigDecimal.ZERO.setScale(2);
        this.totalAmount = BigDecimal.ZERO.setScale(2);
    }

    /** Copies name and price from the product right now and adds the line to this order. */
    public OrderItem addLine(Product product, int quantity) {
        OrderItem item = new OrderItem(this, product, quantity);
        items.add(item);
        subtotal = subtotal.add(item.getLineTotal());
        totalAmount = subtotal;
        return item;
    }

    public void markPaid() {
        this.paymentStatus = PaymentStatus.PAID;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public Long getId() {
        return id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public Long getUserId() {
        return userId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public Instant getOrderDate() {
        return orderDate;
    }

    public String getShippingName() {
        return shippingName;
    }

    public String getShippingPhone() {
        return shippingPhone;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public String getShippingCity() {
        return shippingCity;
    }

    public String getShippingPincode() {
        return shippingPincode;
    }

    public List<OrderItem> getItems() {
        return items;
    }
}
