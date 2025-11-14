package com.example.notification.service;

import com.example.notification.event.PaymentCompletedEvent;
import com.example.notification.event.ShippingCreatedEvent;
import com.example.notification.model.NotificationRequest;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NotificationEventListener {
    
    @Autowired
    private NotificationService notificationService;
    
    @RabbitListener(queues = "payment.completed.queue")
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        System.out.println("Sending payment confirmation notification for order: " + event.getOrderId());
        
        NotificationRequest request = new NotificationRequest();
        request.setType("EMAIL");
        request.setRecipient("customer@example.com");
        request.setSubject("Payment Confirmed - Order #" + event.getOrderId());
        request.setMessage(String.format(
            "Dear Customer,\n\n" +
            "Your payment of $%.2f for order %s has been confirmed.\n" +
            "Transaction ID: %s\n\n" +
            "Thank you for your purchase!\n" +
            "The E-Commerce Team",
            event.getAmount(),
            event.getOrderId(),
            event.getPaymentId()
        ));
        
        notificationService.sendNotification(request);
    }
    
    @RabbitListener(queues = "shipping.created.queue")
    public void handleShippingCreated(ShippingCreatedEvent event) {
        System.out.println("Sending shipping notification for order: " + event.getOrderId());
        
        NotificationRequest request = new NotificationRequest();
        request.setType("EMAIL");
        request.setRecipient("customer@example.com");
        request.setSubject("Order Shipped - Order #" + event.getOrderId());
        request.setMessage(String.format(
            "Dear Customer,\n\n" +
            "Great news! Your order %s has been shipped.\n" +
            "Tracking Number: %s\n\n" +
            "You can track your package using the tracking number above.\n\n" +
            "Thank you for shopping with us!\n" +
            "The E-Commerce Team",
            event.getOrderId(),
            event.getTrackingNumber()
        ));
        
        notificationService.sendNotification(request);
    }
}