package com.example.shipping.event;

import java.util.Date;

public class PaymentCompletedEvent {
    private String paymentId;
    private String orderId;
    private double amount;
    private Date completedAt;
    
    public PaymentCompletedEvent() {}
    
    public PaymentCompletedEvent(String paymentId, String orderId, double amount, Date completedAt) {
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.amount = amount;
        this.completedAt = completedAt;
    }
    
    // Getters and Setters
    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
    
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    
    public Date getCompletedAt() { return completedAt; }
    public void setCompletedAt(Date completedAt) { this.completedAt = completedAt; }
}