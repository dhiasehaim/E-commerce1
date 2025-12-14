package com.example.payment.controller;

import com.example.payment.model.PaymentRequest;
import com.example.payment.model.PaymentResponse;
import com.example.payment.service.PaymentService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Random;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);
    
    @Autowired
    private PaymentService paymentService;
    
    @Autowired
    private Counter httpRequestsCounter;
    
    @Autowired
    private Counter paymentsProcessedCounter;
    
    @Autowired
    private Timer paymentProcessingTimer;

    private final Random random = new Random();
    private int requestCount = 0;

    @PostMapping
    public ResponseEntity<PaymentResponse> processPayment(@RequestBody PaymentRequest request) {
        httpRequestsCounter.increment();
        logger.info("Order ID: {}, Amount: {}, Method: {}", 
                   request.getOrderId(), request.getAmount(), request.getPaymentMethod());
        
        requestCount++;
        logger.debug("Request count: {}", requestCount);
        
        Timer.Sample sample = Timer.start();
        
        try {
            double randomValue = random.nextDouble();
            if (randomValue > 0.9) {
                logger.warn("Simulating slow response (8 seconds)");
                Thread.sleep(8000); 
            }

            PaymentResponse response = paymentService.processPayment(request);
            paymentsProcessedCounter.increment();
            logger.info("Payment ID: {}, Status: {}, Transaction ID: {}", 
                       response.getPaymentId(), response.getStatus(), response.getTransactionId());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Payment processing failed: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(null);
        } finally {
            sample.stop(paymentProcessingTimer);
        }
    }

    @GetMapping("/all")
    public ResponseEntity<List<PaymentResponse>> getAllPayments() {
        httpRequestsCounter.increment();
        logger.info("Getting all payments");
        
        List<PaymentResponse> payments = paymentService.getAllPayments();
        logger.info("Returning {} payments", payments.size());
        
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable String id) {
        httpRequestsCounter.increment();
        logger.info("Getting payment by ID: {}", id);
        
        PaymentResponse response = paymentService.getPayment(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentResponse> getPaymentByOrder(@PathVariable String orderId) {
        httpRequestsCounter.increment();
        logger.info("Getting payment by Order ID: {}", orderId);
        
        PaymentResponse response = paymentService.getPaymentByOrderId(orderId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        logger.debug("Health check endpoint called");
        return ResponseEntity.ok("Payment Service is UP and RUNNING! Guaranteed to return SUCCESS status.");
    }

    @GetMapping("/test")
    public ResponseEntity<PaymentResponse> testPayment() {
        httpRequestsCounter.increment();
        logger.info("========== TEST PAYMENT ENDPOINT ==========");
        
        PaymentRequest testRequest = new PaymentRequest(
            "test-order-" + System.currentTimeMillis(), 
            99.99, 
            "CARD"
        );
        
        PaymentResponse response = paymentService.processPayment(testRequest);
        logger.info("TEST PAYMENT COMPLETED: {}", response.getStatus());
        
        return ResponseEntity.ok(response);
    }
}