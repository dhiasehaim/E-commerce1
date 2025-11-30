package com.example.payment.service;

import com.example.payment.model.Payment;
import com.example.payment.model.PaymentRequest;
import com.example.payment.model.PaymentResponse;
import com.example.payment.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    public PaymentResponse processPayment(PaymentRequest request) {
        System.out.println("🚀 PAYMENT SERVICE: STARTING PAYMENT PROCESSING");
        System.out.println("   📋 Order ID: " + request.getOrderId());
        System.out.println("   💵 Amount: " + request.getAmount());
        System.out.println("   💳 Method: " + request.getPaymentMethod());

        // Create payment - status will be automatically set to SUCCESS by the entity
        Payment payment = new Payment();
        payment.setOrderId(request.getOrderId());
        payment.setAmount(request.getAmount());
        payment.setPaymentMethod(request.getPaymentMethod());
        // No need to set status - it's forced to SUCCESS by the entity

        // Save payment
        Payment savedPayment = paymentRepository.save(payment);

        System.out.println("✅ PAYMENT CREATED SUCCESSFULLY!");
        System.out.println("   🆔 Payment ID: " + savedPayment.getId());
        System.out.println("   ✅ Status: " + savedPayment.getStatus());
        System.out.println("   🔄 Transaction ID: " + savedPayment.getTransactionId());
        System.out.println("   📦 Order ID: " + savedPayment.getOrderId());
        System.out.println("🏁 PAYMENT SERVICE: PROCESSING COMPLETED");

        return PaymentResponse.fromEntity(savedPayment);
    }

    public List<PaymentResponse> getAllPayments() {
        List<Payment> payments = paymentRepository.findAll();
        System.out.println("📊 Retrieved " + payments.size() + " payments from database");
        payments.forEach(payment -> System.out.println(
                "   - " + payment.getId() + ": " + payment.getStatus() + " for order " + payment.getOrderId()));
        return payments.stream()
                .map(PaymentResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public PaymentResponse getPayment(String id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> {
                    System.out.println("❌ Payment not found with id: " + id);
                    return new RuntimeException("Payment not found with id: " + id);
                });
        System.out.println("✅ Retrieved payment by ID: " + id + " - Status: " + payment.getStatus());
        return PaymentResponse.fromEntity(payment);
    }

    public PaymentResponse getPaymentByOrderId(String orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId);
        if (payment == null) {
            System.out.println("❌ Payment not found for order id: " + orderId);
            throw new RuntimeException("Payment not found for order id: " + orderId);
        }
        System.out.println("✅ Retrieved payment by Order ID: " + orderId + " - Status: " + payment.getStatus());
        return PaymentResponse.fromEntity(payment);
    }
}