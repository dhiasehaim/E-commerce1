package com.example.shipping.model;

import java.util.Date;

public class ShippingResponse {
    private String shippingId;
    private String orderId;
    private String customerId;
    private String address;
    private String status;
    private String trackingNumber;
    private Date createdAt;
    
    public static ShippingResponse fromEntity(Shipping shipping) {
        ShippingResponse response = new ShippingResponse();
        response.setShippingId(shipping.getId());
        response.setOrderId(shipping.getOrderId());
        response.setCustomerId(shipping.getCustomerId());
        response.setAddress(shipping.getAddress());
        response.setStatus(shipping.getStatus());
        response.setTrackingNumber(shipping.getTrackingNumber());
        response.setCreatedAt(shipping.getCreatedAt());
        return response;
    }
    
    // Getters and Setters
    public String getShippingId() { return shippingId; }
    public void setShippingId(String shippingId) { this.shippingId = shippingId; }
    
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getTrackingNumber() { return trackingNumber; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }
    
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}