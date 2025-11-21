package com.example.auth.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserContext {
    private String username;
    private List<String> roles;
    private Long customerId;
    
    public UserContext(String username, List<String> roles, Long customerId) {
        this.username = username;
        this.roles = roles;
        this.customerId = customerId;
    }
    
    public boolean isAdmin() {
        return roles.contains("ROLE_ADMIN");
    }
    
    public boolean isStaff() {
        return roles.contains("ROLE_STAFF");
    }
    
    public boolean isCustomer() {
        return roles.contains("ROLE_CUSTOMER");
    }
}