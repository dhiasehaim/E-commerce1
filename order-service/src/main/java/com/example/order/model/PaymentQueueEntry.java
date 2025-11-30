package com.example.order.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_queue")
public class PaymentQueueEntry {

    @Id
    private String id;

    @Column(nullable = false)
    private String orderId;

    @Column(nullable = false)
    private double amount;

    @Column(nullable = false)
    private String paymentMethod;

    @Column(nullable = false)
    private int attempts = 0;

    @Column(nullable = false)
    private String status = "PENDING";

    @Column(nullable = false)
    private Instant createdAt;

    public PaymentQueueEntry() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = Instant.now();
    }

    public static PaymentQueueEntry fromOrder(String orderId, double amount, String paymentMethod) {
        PaymentQueueEntry e = new PaymentQueueEntry();
        e.setOrderId(orderId);
        e.setAmount(amount);
        e.setPaymentMethod(paymentMethod);
        return e;
    }

    // getters & setters
    // ... (generate all)
    // For brevity include getters/setters in your IDE
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
