package com.example.auth.model;

import lombok.Data;

import java.util.List;

@Data
public class LoginResponse {
    private String token;
    private String type = "Bearer";
    private String username;
    private List<String> roles;
    private Long customerId;  // For customer role
    
    public LoginResponse(String token, String username, List<String> roles, Long customerId) {
        this.token = token;
        this.username = username;
        this.roles = roles;
        this.customerId = customerId;
    }
}