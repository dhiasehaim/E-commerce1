package com.example.shipping.service;

import com.example.shipping.model.Shipping;
import com.example.shipping.model.ShippingRequest;
import com.example.shipping.model.ShippingResponse;
import com.example.shipping.repository.ShippingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;  // ADD THIS IMPORT
import java.util.stream.Collectors;  // ADD THIS IMPORT

@Service
public class ShippingService {
    
    @Autowired
    private ShippingRepository shippingRepository;
    
    public ShippingResponse createShipping(ShippingRequest request) {
        Shipping shipping = new Shipping(
            request.getOrderId(),
            request.getRecipientName(),
            request.getCustomerId(),
            request.getAddress(),
            "PENDING"
        );
        
        Shipping savedShipping = shippingRepository.save(shipping);
        return ShippingResponse.fromEntity(savedShipping);
    }
    
    // ADD THIS - Get all shippings
    public List<ShippingResponse> getAllShippings() {
        List<Shipping> shippings = shippingRepository.findAll();
        return shippings.stream()
            .map(ShippingResponse::fromEntity)
            .collect(Collectors.toList());
    }
    
    public ShippingResponse getShippingByOrderId(String orderId) {
        Shipping shipping = shippingRepository.findByOrderId(orderId)
            .orElseThrow(() -> new RuntimeException("Shipping not found for order: " + orderId));
        return ShippingResponse.fromEntity(shipping);
    }
    
    public ShippingResponse getShipping(String id) {
        Shipping shipping = shippingRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Shipping not found with id: " + id));
        return ShippingResponse.fromEntity(shipping);
    }
}