package com.example.payment.service;

import com.example.payment.event.OrderCreatedEvent;
import com.example.payment.event.PaymentCompletedEvent;
import com.example.payment.model.PaymentRequest;
import com.example.payment.model.PaymentResponse;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PaymentEventListener {
    
    @Autowired
    private PaymentService paymentService;
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    @RabbitListener(queues = "order.created.queue")
    public void handleOrderCreated(OrderCreatedEvent event) {
        System.out.println("Processing payment for order: " + event.getOrderId());
        
        // Process payment
        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setOrderId(event.getOrderId());
        paymentRequest.setAmount(event.getTotalAmount());
        paymentRequest.setPaymentMethod(event.getPaymentMethod());
        
        PaymentResponse paymentResponse = paymentService.processPayment(paymentRequest);
        
        // Publish payment completed event
        if ("COMPLETED".equals(paymentResponse.getStatus())) {
            PaymentCompletedEvent paymentEvent = new PaymentCompletedEvent(
                paymentResponse.getPaymentId(),
                paymentResponse.getOrderId(),
                paymentResponse.getAmount(),
                paymentResponse.getCreatedAt()
            );
            
            rabbitTemplate.convertAndSend("payment.exchange", "payment.completed", paymentEvent);
            System.out.println("Payment completed event published for order: " + event.getOrderId());
        }
    }
}