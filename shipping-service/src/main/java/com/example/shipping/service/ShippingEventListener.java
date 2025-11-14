package com.example.shipping.service;

import com.example.shipping.event.PaymentCompletedEvent;
import com.example.shipping.model.ShippingRequest;
import com.example.shipping.model.ShippingResponse;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ShippingEventListener {
    
    @Autowired
    private ShippingService shippingService;
    
    @RabbitListener(queues = "payment.completed.queue")
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        System.out.println("Creating shipping for order: " + event.getOrderId());
        
        try {
            ShippingRequest request = new ShippingRequest();
            request.setOrderId(event.getOrderId());
            request.setCustomerId("default-customer"); // In real app, get from order service
            request.setAddress("123 Main St, City, Country");
            
            ShippingResponse response = shippingService.createShipping(request);
            System.out.println("Shipping created with tracking: " + response.getTrackingNumber());
        } catch (Exception e) {
            System.err.println("Error creating shipping for order: " + event.getOrderId());
            e.printStackTrace();
        }
    }
}