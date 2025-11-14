package com.example.shipping.controller;

import com.example.shipping.model.ShippingRequest;
import com.example.shipping.model.ShippingResponse;
import com.example.shipping.service.ShippingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shipping")
public class ShippingController {
    
    @Autowired
    private ShippingService shippingService;
    
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