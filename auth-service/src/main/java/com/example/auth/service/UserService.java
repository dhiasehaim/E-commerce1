package com.example.auth.service;

import com.example.auth.model.Role;
import com.example.auth.model.User;
import com.example.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    public User registerUser(String username, String password, String email, List<Role> roles, Long customerId) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already exists");
        }
        
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setRoles(roles);
        user.setCustomerId(customerId);
        
        return userRepository.save(user);
    }
    
    public Optional<User> authenticate(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent() && passwordEncoder.matches(password, userOpt.get().getPassword())) {
            return userOpt;
        }
        return Optional.empty();
    }
    
    // Initialize with some demo users
    public void initializeDemoUsers() {
        if (userRepository.count() == 0) {
            // Admin user
            registerUser("admin", "admin123", "admin@example.com", 
                        Arrays.asList(Role.ROLE_ADMIN), null);
            
            // Staff user  
            registerUser("staff", "staff123", "staff@example.com",
                        Arrays.asList(Role.ROLE_STAFF), null);
            
            // Customer user (customerId 1)
            registerUser("customer1", "customer123", "customer1@example.com",
                        Arrays.asList(Role.ROLE_CUSTOMER), 1L);
        }
    }
}