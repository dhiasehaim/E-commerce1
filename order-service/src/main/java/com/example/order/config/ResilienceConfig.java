package com.example.order.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Configuration
public class ResilienceConfig {

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50) // 50% failure rate threshold
                .slowCallRateThreshold(50) // 50% slow call threshold
                .slowCallDurationThreshold(Duration.ofSeconds(5)) // 5s timeout as specified
                .waitDurationInOpenState(Duration.ofSeconds(30)) // 30s reset timeout
                .permittedNumberOfCallsInHalfOpenState(1) // 1 call in half-open state
                .minimumNumberOfCalls(10) // min 10 calls before evaluation
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(20) // last 20 calls
                .build();

        return CircuitBreakerRegistry.of(config);
    }

    @Bean
    public RetryRegistry retryRegistry() {
        // Exponential backoff with ±30% random jitter
        IntervalFunction intervalFunction = IntervalFunction.ofExponentialRandomBackoff(
                1000L, // initial interval in milliseconds (1 second)
                2.0,   // multiplier
                0.3    // ±30% jitter
        );

        RetryConfig config = RetryConfig.custom()
                .maxAttempts(4) // 1 initial + 3 retries = 4 total attempts
                .intervalFunction(intervalFunction)
                .retryExceptions(
                        IOException.class,
                        TimeoutException.class,
                        WebClientResponseException.InternalServerError.class,
                        WebClientResponseException.ServiceUnavailable.class,
                        WebClientResponseException.GatewayTimeout.class
                )
                .ignoreExceptions(
                        WebClientResponseException.BadRequest.class,
                        WebClientResponseException.NotFound.class,
                        WebClientResponseException.Unauthorized.class
                )
                .build();

        return RetryRegistry.of(config);
    }
}