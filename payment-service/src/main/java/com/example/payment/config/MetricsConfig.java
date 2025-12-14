package com.example.payment.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {
    
    @Bean
    public Counter paymentsProcessedCounter(MeterRegistry registry) {
        return Counter.builder("payments.processed.total")
                .description("Total number of payments processed")
                .tag("service", "payment-service")
                .register(registry);
    }
    
    @Bean
    public Counter paymentsFailedCounter(MeterRegistry registry) {
        return Counter.builder("payments.failed.total")
                .description("Total number of failed payments")
                .tag("service", "payment-service")
                .register(registry);
    }
    
    @Bean
    public Counter paymentsSuccessfulCounter(MeterRegistry registry) {
        return Counter.builder("payments.successful.total")
                .description("Total number of successful payments")
                .tag("service", "payment-service")
                .register(registry);
    }
    
    @Bean
    public Timer paymentProcessingTimer(MeterRegistry registry) {
        return Timer.builder("payment.processing.duration")
                .description("Time taken to process a payment")
                .tag("service", "payment-service")
                .register(registry);
    }
    
    @Bean
    public Counter httpRequestsCounter(MeterRegistry registry) {
        return Counter.builder("http.requests.total")
                .description("Total HTTP requests")
                .tag("service", "payment-service")
                .register(registry);
    }
    
    @Bean
    public Counter externalGatewayCallsCounter(MeterRegistry registry) {
        return Counter.builder("payments.external.gateway.calls")
                .description("Calls to external payment gateway")
                .tag("service", "payment-service")
                .register(registry);
    }
}