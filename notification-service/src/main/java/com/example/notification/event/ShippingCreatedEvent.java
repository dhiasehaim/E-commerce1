package com.example.notification.event;

import java.util.Date;

public class ShippingCreatedEvent {
    private String shippingId;
    private String orderId;
    private String trackingNumber;
    private Date createdAt;
    
    public ShippingCreatedEvent() {}
    
    public ShippingCreatedEvent(String shippingId, String orderId, String trackingNumber, Date createdAt) {
        this.shippingId = shippingId;
        this.orderId = orderId;
        this.trackingNumber = trackingNumber;
        this.createdAt = createdAt;
    }
    
    // Getters and Setters
    public String getShippingId() { return shippingId; }
    public void setShippingId(String shippingId) { this.shippingId = shippingId; }
    
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    
    public String getTrackingNumber() { return trackingNumber; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }
    
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}