package com.example.order.client;

import com.example.order.dto.PaymentRequestDto;
import com.example.order.dto.PaymentResponseDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
public class PaymentClient {
    private static final Logger logger = LoggerFactory.getLogger(PaymentClient.class);
    
    private final WebClient webClient;
    private final String serviceName = "payment-service";

    public PaymentClient(WebClient.Builder builder,
                         @Value("${services.payment.base-url:http://payment-service:8084}") String baseUrl) {
        this.webClient = builder.baseUrl(baseUrl).build();
    }

    @CircuitBreaker(name = "paymentService", fallbackMethod = "fallbackPayment")
    @Retry(name = "paymentService")
    public Mono<PaymentResponseDto> processPayment(PaymentRequestDto request, String authHeader) {
        String correlationId = MDC.get("correlationId");
        
        logger.info("Calling payment service for order: {}, correlationId: {}", 
                   request.getOrderId(), correlationId);
        
        return webClient.post()
                .uri("/api/payments")
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .header("X-Correlation-ID", correlationId) // Propagate correlation ID
                .headers(h -> { 
                    if (authHeader != null) h.set(HttpHeaders.AUTHORIZATION, authHeader); 
                })
                .bodyValue(request)
                .retrieve()
                .bodyToMono(PaymentResponseDto.class)
                .timeout(Duration.ofSeconds(5))
                .doOnSuccess(response -> 
                    logger.info("Payment successful for order: {}, paymentId: {}", 
                               request.getOrderId(), response.getPaymentId()))
                .doOnError(error -> 
                    logger.error("Payment failed for order: {}, error: {}", 
                                request.getOrderId(), error.getMessage()));
    }
    public Mono<PaymentResponseDto> fallbackPayment(PaymentRequestDto request, String authHeader, Throwable t) {
        String correlationId = MDC.get("correlationId");
        
        logger.warn("FALLBACK: Payment service unavailable for order: {}, correlationId: {}, error: {}", 
                   request.getOrderId(), correlationId, t.getMessage());
        
        PaymentResponseDto fallback = new PaymentResponseDto();
        fallback.setOrderId(request.getOrderId());
        fallback.setStatus("PENDING_PAYMENT");
        fallback.setPaymentId("FALLBACK-" + System.currentTimeMillis());
        fallback.setAmount(0.0);
        return Mono.just(fallback);
    }
}