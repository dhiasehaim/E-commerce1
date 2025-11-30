package com.example.auth.controller;

import com.example.auth.model.LoginRequest;
import com.example.auth.model.LoginResponse;
import com.example.auth.model.User;
import com.example.auth.service.AuthService;
import com.example.auth.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
        Long customerId = request.getCustomerId() != null ? request.getCustomerId() : System.currentTimeMillis();
        String email = request.getEmail() != null ? request.getEmail() : request.getUsername() + "@example.com";
        
        User user = authService.registerCustomer(request.getUsername(), request.getPassword(), email, customerId);
        return ResponseEntity.ok(user);
    }
    
    // UPDATE THIS METHOD TO INCLUDE ROLES IN RESPONSE:
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        boolean isValid = authService.validateToken(token);
        
        if (isValid) {
            String username = authService.getUsernameFromToken(token);
            List<String> roles = authService.extractRoles(token);
            Long customerId = authService.extractCustomerId(token);
            
            return ResponseEntity.ok()
                .header("X-User-Roles", String.join(",", roles))
                .header("X-Customer-Id", customerId != null ? customerId.toString() : "")
                .header("X-Username", username)
                .body(Map.of(
                    "valid", true,
                    "username", username,
                    "roles", roles,
                    "customerId", customerId
                ));
        }
        return ResponseEntity.status(401).body(Map.of("valid", false));
    }
    
    @PostMapping("/init-demo")
    public ResponseEntity<String> initializeDemoUsers() {
        userService.initializeDemoUsers();
        return ResponseEntity.ok("Demo users initialized");
    }
}