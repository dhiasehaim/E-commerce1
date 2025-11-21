package com.example.shipping.controller;

import com.example.shipping.model.ShippingRequest;
import com.example.shipping.model.ShippingResponse;
import com.example.shipping.service.ShippingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;  // ADD THIS IMPORT

@RestController
@RequestMapping("/api/shipping")
public class ShippingController {
    
    @Autowired
    private ShippingService shippingService;
    
    // ADD THIS - Get all shippings
    @GetMapping("/all")
    public ResponseEntity<List<ShippingResponse>> getAllShippings() {
        List<ShippingResponse> shippings = shippingService.getAllShippings();
        return ResponseEntity.ok(shippings);
    }
    
    @PostMapping
    public ResponseEntity<ShippingResponse> createShipping(@RequestBody ShippingRequest request) {
        ShippingResponse response = shippingService.createShipping(request);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ShippingResponse> getShipping(@PathVariable String id) {
        ShippingResponse response = shippingService.getShipping(id);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/order/{orderId}")
    public ResponseEntity<ShippingResponse> getShippingByOrder(@PathVariable String orderId) {
        ShippingResponse response = shippingService.getShippingByOrderId(orderId);
        return ResponseEntity.ok(response);
    }
}