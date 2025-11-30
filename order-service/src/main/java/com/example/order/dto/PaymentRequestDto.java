package com.example.order.dto;

public class PaymentRequestDto {
    private String orderId;
    private double amount;
    private String paymentMethod;

    public PaymentRequestDto() {}

    public PaymentRequestDto(String orderId, double amount, String paymentMethod) {
        this.orderId = orderId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
    }

    // getters/setters
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
}
