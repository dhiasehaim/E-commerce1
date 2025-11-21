package com.example.auth.model;

import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String password;
    private String email;  // For registration
    private Long customerId; // For customer registration
    
    // Constructors
    public LoginRequest() {}
    
    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }
    
    public LoginRequest(String username, String password, String email, Long customerId) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.customerId = customerId;
    }
}