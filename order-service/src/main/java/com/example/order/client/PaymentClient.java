package com.example.order.client;

import com.example.order.dto.PaymentRequestDto;
import com.example.order.dto.PaymentResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
public class PaymentClient {
    private final WebClient webClient;

    public PaymentClient(WebClient.Builder builder,
                         @Value("${services.payment.base-url:http://payment-service:8084}") String baseUrl) {
        this.webClient = builder.baseUrl(baseUrl).build();
    }

    public Mono<PaymentResponseDto> processPayment(PaymentRequestDto request, String authHeader) {
        return webClient.post()
                .uri("/api/payments")
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .headers(h -> { if (authHeader != null) h.set(HttpHeaders.AUTHORIZATION, authHeader); })
                .bodyValue(request)
                .retrieve()
                .bodyToMono(PaymentResponseDto.class)
                // STEP 3:
                .timeout(Duration.ofSeconds(5))
                // STEP 4: 
                .onErrorResume(throwable -> {
                    System.err.println("Payment service call failed, using fallback: " + throwable.getMessage());
                    return Mono.just(createFallbackResponse(request.getOrderId()));
                });
    }
    private PaymentResponseDto createFallbackResponse(String orderId) {
        System.out.println("FALLBACK: Creating fallback response for order " + orderId);
        
        PaymentResponseDto fallback = new PaymentResponseDto();
        fallback.setOrderId(orderId);
        fallback.setStatus("PENDING_PAYMENT"); 
        fallback.setPaymentId("FALLBACK-" + System.currentTimeMillis());
        fallback.setAmount(0.0);
        return fallback;
    }
}