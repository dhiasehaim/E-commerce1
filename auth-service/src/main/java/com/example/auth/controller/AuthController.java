package com.example.auth.controller;

import com.example.auth.model.LoginRequest;
import com.example.auth.model.LoginResponse;
import com.example.auth.model.User;
import com.example.auth.service.AuthService;
import com.example.auth.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {
    
    @Autowired
    private AuthService authService;
    
    @Autowired
    private UserService userService;
    
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/register")
    public ResponseEntity<User> registerCustomer(@RequestBody LoginRequest request) {
        // Use customerId from request or generate temporary one
        Long customerId = request.getCustomerId() != null ? request.getCustomerId() : System.currentTimeMillis();
        String email = request.getEmail() != null ? request.getEmail() : request.getUsername() + "@example.com";
        
        User user = authService.registerCustomer(request.getUsername(), request.getPassword(), email, customerId);
        return ResponseEntity.ok(user);
    }
    
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        boolean isValid = authService.validateToken(token);
        String username = isValid ? authService.getUsernameFromToken(token) : null;
        
        return ResponseEntity.ok(Map.of(
            "valid", isValid,
            "username", username
        ));
    }
    
    @PostMapping("/init-demo")
    public ResponseEntity<String> initializeDemoUsers() {
        userService.initializeDemoUsers();
        return ResponseEntity.ok("Demo users initialized");
    }
}