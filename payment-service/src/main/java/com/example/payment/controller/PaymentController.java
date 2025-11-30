package com.example.payment.controller;

import com.example.payment.model.PaymentRequest;
import com.example.payment.model.PaymentResponse;
import com.example.payment.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Random;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;
    
    private final Random random = new Random();
    private int requestCount = 0;

    @PostMapping
    public ResponseEntity<PaymentResponse> processPayment(@RequestBody PaymentRequest request) throws Exception {
        System.out.println(" ========== PAYMENT CONTROLLER: NEW REQUEST ==========");
        System.out.println("    Received Order ID: " + request.getOrderId());
        System.out.println("    Received Amount: " + request.getAmount());
        System.out.println("    Received Method: " + request.getPaymentMethod());
        
        requestCount++;
        System.out.println("    Request Count: " + requestCount);

        try {
            // STEP 2: SIMULATE RANDOM FAILURES FOR RETRY TESTING (50% failure rate)
            double randomValue = random.nextDouble();
            System.out.println("    Random value: " + randomValue);
            
            if (randomValue < 0.5) {
                System.out.println("    🔥 SIMULATED FAILURE - Testing retry mechanism");
                throw new RuntimeException("Simulated transient failure - Service temporarily unavailable");
            }
            
            // STEP 3: SIMULATE SLOW RESPONSE FOR TIMEOUT TESTING (10% slow responses)
            if (randomValue > 0.9) {
                System.out.println("    ⏰ SIMULATING SLOW RESPONSE (8 seconds)");
                Thread.sleep(8000); // 8 seconds delay - should trigger timeout
            }

            PaymentResponse response = paymentService.processPayment(request);

            System.out.println(" ========== PAYMENT CONTROLLER: SUCCESS RESPONSE ==========");
            System.out.println("    Payment ID: " + response.getPaymentId());
            System.out.println("    Status: " + response.getStatus());
            System.out.println("    Transaction ID: " + response.getTransactionId());
            System.out.println("    Order ID: " + response.getOrderId());
            System.out.println("    Created: " + response.getCreatedAt());
            System.out.println(" ========== RESPONSE SENT TO CLIENT ==========");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.out.println(" ========== PAYMENT CONTROLLER: ERROR ==========");
            System.out.println("    Error: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    // Keep your existing methods unchanged
    @GetMapping("/all")
    public ResponseEntity<List<PaymentResponse>> getAllPayments() {
        System.out.println(" Getting all payments");
        List<PaymentResponse> payments = paymentService.getAllPayments();
        System.out.println(" Returning " + payments.size() + " payments");
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable String id) {
        System.out.println(" Getting payment by ID: " + id);
        PaymentResponse response = paymentService.getPayment(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentResponse> getPaymentByOrder(@PathVariable String orderId) {
        System.out.println(" Getting payment by Order ID: " + orderId);
        PaymentResponse response = paymentService.getPaymentByOrderId(orderId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        System.out.println(" Health check endpoint called");
        return ResponseEntity.ok("Payment Service is UP and RUNNING! Guaranteed to return SUCCESS status.");
    }

    @GetMapping("/test")
    public ResponseEntity<PaymentResponse> testPayment() {
        System.out.println(" ========== TEST PAYMENT ENDPOINT ==========");
        PaymentRequest testRequest = new PaymentRequest("test-order-" + System.currentTimeMillis(), 99.99, "CARD");
        PaymentResponse response = paymentService.processPayment(testRequest);
        System.out.println(" TEST PAYMENT COMPLETED: " + response.getStatus());
        return ResponseEntity.ok(response);
    }
}