package com.example.order.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {
    
    @Bean
    public Counter ordersCreatedCounter(MeterRegistry registry) {
        return Counter.builder("orders.created.total")
                .description("Total number of orders created")
                .tag("service", "order-service")
                .register(registry);
    }
    
    @Bean
    public Counter ordersRetrievedCounter(MeterRegistry registry) {
        return Counter.builder("orders.retrieved.total")
                .description("Total number of orders retrieved")
                .tag("service", "order-service")
                .register(registry);
    }
    
    @Bean
    public Counter ordersFailedCounter(MeterRegistry registry) {
        return Counter.builder("orders.failed.total")
                .description("Total number of failed orders")
                .tag("service", "order-service")
                .register(registry);
    }
    
    @Bean
    public Timer orderProcessingTimer(MeterRegistry registry) {
        return Timer.builder("order.processing.duration")
                .description("Time taken to process an order")
                .tag("service", "order-service")
                .register(registry);
    }
    
    @Bean
    public Counter httpRequestsCounter(MeterRegistry registry) {
        return Counter.builder("http.requests.total")
                .description("Total HTTP requests")
                .tag("service", "order-service")
                .register(registry);
    }
    
    @Bean
    public Counter paymentStatusCounter(MeterRegistry registry) {
        return Counter.builder("orders.payment.status")
                .description("Order payment status counts")
                .tag("service", "order-service")
                .register(registry);
    }
}