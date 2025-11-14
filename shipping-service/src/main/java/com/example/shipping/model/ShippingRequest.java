package com.example.shipping.model;

public class ShippingRequest {
    private String orderId;
    private String customerId;
    private String recipientName; 
    private String address;
    
    public ShippingRequest() {}
    
    public ShippingRequest(String orderId, String customerId, String address, String recipientName) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.recipientName = recipientName; 
        this.address = address;
    }
    
    // Getters and Setters
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
     public String getRecipientName() { return recipientName; }
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
}