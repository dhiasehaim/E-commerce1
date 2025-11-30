package com.example.order.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String productName;
    private int quantity;
    private double price;
    private String paymentMethod;     
    private String paymentStatus;    
    private String paymentId;       
    private Long customerId;  
}