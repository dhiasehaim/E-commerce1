package com.example.auth.service;

import com.example.auth.model.LoginRequest;
import com.example.auth.model.LoginResponse;
import com.example.auth.model.Role;
import com.example.auth.model.User;
import com.example.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class AuthService {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private JwtService jwtService;
    
    @Autowired
    private UserRepository userRepository;
    
    public LoginResponse login(LoginRequest request) {
        User user = userService.authenticate(request.getUsername(), request.getPassword())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));
        
        var roles = user.getRoles().stream()
                .map(Role::name)
                .collect(Collectors.toList());
        
        String token = jwtService.generateToken(user.getUsername(), roles, user.getCustomerId());
        
        return new LoginResponse(token, user.getUsername(), roles, user.getCustomerId());
    }
    
    public User registerCustomer(String username, String password, String email, Long customerId) {
        return userService.registerUser(username, password, email, 
                                      java.util.List.of(Role.ROLE_CUSTOMER), customerId);
    }
    
    public boolean validateToken(String token) {
        return jwtService.validateToken(token);
    }
    
    public String getUsernameFromToken(String token) {
        return jwtService.extractUsername(token);
    }
}