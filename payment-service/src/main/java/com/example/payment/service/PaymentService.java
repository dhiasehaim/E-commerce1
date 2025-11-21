package com.example.payment.service;

import com.example.payment.model.Payment;
import com.example.payment.model.PaymentRequest;
import com.example.payment.model.PaymentResponse;
import com.example.payment.repository.PaymentRepository;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {
    
    @Autowired
    private PaymentRepository paymentRepository;
    
    public PaymentResponse processPayment(PaymentRequest request) {
        // Simulate payment processing
        Payment payment = new Payment(
            request.getOrderId(),
            request.getAmount(),
            request.getPaymentMethod(),
            "COMPLETED" // Simulate successful payment
        );
        
        Payment savedPayment = paymentRepository.save(payment);
        return PaymentResponse.fromEntity(savedPayment);
    }
    public List<PaymentResponse> getAllPayments() {
        List<Payment> payments = paymentRepository.findAll();
        return payments.stream()
            .map(PaymentResponse::fromEntity)
            .collect(Collectors.toList());
    }
    public PaymentResponse getPayment(String id) {
        Payment payment = paymentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Payment not found with id: " + id));
        return PaymentResponse.fromEntity(payment);
    }
    
    public PaymentResponse getPaymentByOrderId(String orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId);
        if (payment == null) {
            throw new RuntimeException("Payment not found for order: " + orderId);
        }
        return PaymentResponse.fromEntity(payment);
    }
}