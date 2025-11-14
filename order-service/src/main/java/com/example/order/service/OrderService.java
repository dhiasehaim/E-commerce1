package com.example.order.service;

import com.example.order.model.Order;
import com.example.order.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderService {
    
    @Autowired
    private OrderRepository orderRepository;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    public Order createOrder(Order order) {
        Order savedOrder = orderRepository.save(order);
        
        // PHASE 2: If payment method provided, call Payment Service
        if (order.getPaymentMethod() != null && !order.getPaymentMethod().isEmpty()) {
            callPaymentService(savedOrder);
        }
        
        return savedOrder;
    }
    
    private void callPaymentService(Order order) {
        try {
            String paymentUrl = "http://localhost:8084/api/payments";
            
            // Prepare payment request
            Map<String, Object> paymentRequest = new HashMap<>();
            paymentRequest.put("orderId", order.getId().toString());
            paymentRequest.put("amount", order.getPrice() * order.getQuantity());
            paymentRequest.put("paymentMethod", order.getPaymentMethod());
            
            // Call Payment Service synchronously
            Map<String, Object> paymentResponse = restTemplate.postForObject(
                paymentUrl, paymentRequest, Map.class);
            
            // Update order with payment info
            if (paymentResponse != null) {
                order.setPaymentStatus((String) paymentResponse.get("status"));
                order.setPaymentId((String) paymentResponse.get("paymentId"));
                orderRepository.save(order);
            }
            
        } catch (Exception e) {
            System.err.println("Payment service call failed: " + e.getMessage());
            order.setPaymentStatus("FAILED");
            orderRepository.save(order);
        }
    }
    
    // Your existing methods...
    public List<Order> getAllOrders() { return orderRepository.findAll(); }
    public Order getOrder(Long id) { return orderRepository.findById(id).orElse(null); }
    public Order updateOrder(Long id, Order order) { 
        order.setId(id);
        return orderRepository.save(order); 
    }
    public void deleteOrder(Long id) { orderRepository.deleteById(id); }
}