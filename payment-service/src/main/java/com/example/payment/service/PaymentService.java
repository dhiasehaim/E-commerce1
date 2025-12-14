package com.example.payment.service;

import com.example.payment.model.Payment;
import com.example.payment.model.PaymentRequest;
import com.example.payment.model.PaymentResponse;
import com.example.payment.repository.PaymentRepository;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
@Transactional
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private RetryRegistry retryRegistry;

    // Counter to track retry attempts
    private final AtomicInteger attemptCounter = new AtomicInteger(1);

    public PaymentResponse processPayment(PaymentRequest request) {
        System.out.println(" PAYMENT SERVICE: STARTING PAYMENT PROCESSING");
        System.out.println("    Order ID: " + request.getOrderId());
        System.out.println("    Amount: " + request.getAmount());
        System.out.println("  Method: " + request.getPaymentMethod());

        // Reset attempt counter
        attemptCounter.set(1);

        try {
            Retry retry = retryRegistry.retry("paymentGatewayRetry");
            Supplier<PaymentResponse> retryablePayment = Retry.decorateSupplier(
                    retry,
                    () -> processSinglePaymentAttempt(request));
            PaymentResponse response = retryablePayment.get();

            System.out.println(" PAYMENT PROCESSING COMPLETED");
            return response;

        } catch (Exception e) {
            System.err.println(" ALL RETRY ATTEMPTS FAILED: " + e.getMessage());
            return fallbackProcessPayment(request, "All retry attempts failed: " + e.getMessage());
        }
    }
    private PaymentResponse processSinglePaymentAttempt(PaymentRequest request) {
        int attempt = attemptCounter.getAndIncrement();
        System.out.println("\n  PAYMENT ATTEMPT #" + attempt);
        simulateTransientFailure(attempt);

        try {
            ExternalGatewayResponse gatewayResponse = callExternalPaymentGateway(request)
                    .block(Duration.ofSeconds(5)); // 5s timeout as per requirement

            if (gatewayResponse.isSuccess()) {
                System.out.println("    Attempt #" + attempt + " SUCCESS");
                return createSuccessfulPayment(request, gatewayResponse);
            } else {
                System.out.println("    Attempt #" + attempt + " FAILED: Gateway returned failure");
                throw new RuntimeException("External payment gateway returned failure");
            }

        } catch (Exception e) {
            System.out.println("    Attempt #" + attempt + " FAILED: " + e.getMessage());
            throw new RuntimeException("Payment attempt failed", e);
        }
    }

    private void simulateTransientFailure(int attempt) {
        double random = Math.random();
        if (attempt == 1) {
            if (random < 0.5) {
                System.out.println("  Simulating network error (attempt " + attempt + ")");
                throw new RuntimeException("Network connection lost");
            }
        } else if (attempt == 2) {
            if (random < 0.5) {
                System.out.println(" Simulating timeout error (attempt " + attempt + ")");
                throw new RuntimeException("Request timeout");
            }
        } else if (attempt == 3) {
            if (random < 0.5) {
                System.out.println(" Simulating 503 Service Unavailable (attempt " + attempt + ")");
                throw new RuntimeException("Service temporarily unavailable - HTTP 503");
            }
        }
        System.out.println("    Simulating success for attempt " + attempt);
    }

    private Mono<ExternalGatewayResponse> callExternalPaymentGateway(PaymentRequest request) {
        System.out.println("Calling external payment gateway...");
        return Mono.fromCallable(() -> {
            Thread.sleep(1000);

            ExternalGatewayResponse response = new ExternalGatewayResponse();
            response.setSuccess(true);
            response.setTransactionId("TXN-" + System.currentTimeMillis() + "-" + attemptCounter.get());
            response.setMessage("Payment processed successfully");
            return response;
        });
    }

    private PaymentResponse createSuccessfulPayment(PaymentRequest request, ExternalGatewayResponse gatewayResponse) {
        Payment payment = new Payment();
        payment.setOrderId(request.getOrderId());
        payment.setAmount(request.getAmount());
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setStatus("SUCCESS");
        payment.setTransactionId(gatewayResponse.getTransactionId());

        Payment savedPayment = paymentRepository.save(payment);

        System.out.println(" PAYMENT CREATED SUCCESSFULLY!");
        System.out.println("   Payment ID: " + savedPayment.getId());
        System.out.println("    Status: " + savedPayment.getStatus());
        System.out.println("  Transaction ID: " + savedPayment.getTransactionId());

        return PaymentResponse.fromEntity(savedPayment);
    }

    private PaymentResponse fallbackProcessPayment(PaymentRequest request, String errorReason) {
        System.out.println("\n FALLBACK ACTIVATED: " + errorReason);
        System.out.println("    Queueing payment for order " + request.getOrderId() + " for later processing");
        Payment payment = new Payment();
        payment.setOrderId(request.getOrderId());
        payment.setAmount(request.getAmount());
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setStatus("PENDING_PAYMENT");
        payment.setTransactionId("QUEUED-" + System.currentTimeMillis());

        Payment savedPayment = paymentRepository.save(payment);

        System.out.println("    Payment queued with status: PENDING_PAYMENT");
        System.out.println("    Payment ID: " + savedPayment.getId());

        return PaymentResponse.fromEntity(savedPayment);
    }

    public List<PaymentResponse> getAllPayments() {
        List<Payment> payments = paymentRepository.findAll();
        System.out.println(" Retrieved " + payments.size() + " payments from database");
        payments.forEach(payment -> System.out.println(
                "   - " + payment.getId() + ": " + payment.getStatus() + " for order " + payment.getOrderId()));
        return payments.stream()
                .map(PaymentResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public PaymentResponse getPayment(String id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> {
                    System.out.println(" Payment not found with id: " + id);
                    return new RuntimeException("Payment not found with id: " + id);
                });
        System.out.println(" Retrieved payment by ID: " + id + " - Status: " + payment.getStatus());
        return PaymentResponse.fromEntity(payment);
    }

    public PaymentResponse getPaymentByOrderId(String orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId);
        if (payment == null) {
            System.out.println(" Payment not found for order id: " + orderId);
            throw new RuntimeException("Payment not found for order id: " + orderId);
        }
        System.out.println(" Retrieved payment by Order ID: " + orderId + " - Status: " + payment.getStatus());
        return PaymentResponse.fromEntity(payment);
    }


    private static class ExternalGatewayResponse {
        private boolean success;
        private String transactionId;
        private String message;

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getTransactionId() {
            return transactionId;
        }

        public void setTransactionId(String transactionId) {
            this.transactionId = transactionId;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}