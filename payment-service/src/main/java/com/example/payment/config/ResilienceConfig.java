package com.example.payment.config;

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
    public RetryRegistry retryRegistry() {
        IntervalFunction intervalFunction = IntervalFunction.ofExponentialRandomBackoff(
                Duration.ofSeconds(1).toMillis(),
                2.0,  
                0.3    
        );

        RetryConfig config = RetryConfig.custom()
                .maxAttempts(4) 
                .intervalFunction(intervalFunction)
                //retry on these exceptions(Newly transient errors)
                .retryExceptions(
                        IOException.class,            
                        TimeoutException.class,
                        WebClientResponseException.InternalServerError.class,
                        WebClientResponseException.ServiceUnavailable.class, 
                        WebClientResponseException.GatewayTimeout.class, 
                        WebClientResponseException.BadGateway.class     
                )
                //Dont retry on these exceptions(Client errors)
                .ignoreExceptions(
                        WebClientResponseException.BadRequest.class,     
                        WebClientResponseException.Unauthorized.class,  
                        WebClientResponseException.Forbidden.class,
                        WebClientResponseException.NotFound.class,   
                        WebClientResponseException.MethodNotAllowed.class 
                )
                .failAfterMaxAttempts(true) 
                .build();

        return RetryRegistry.of(config);
    }
}