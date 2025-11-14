package com.example.shipping.model;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "shippings")
public class Shipping {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    private String orderId;
    private String customerId;
    private String address;
    private String recipientName;
    private String status;
    private String trackingNumber;
    
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;
    
    public Shipping() {}
    
    public Shipping(String orderId, String customerId, String address, String status, String recipientName) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.address = address;
        this.recipientName = recipientName;
        this.status = status;
        this.trackingNumber = java.util.UUID.randomUUID().toString();
        this.createdAt = new Date();
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }
    
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