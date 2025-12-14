package com.example.order.service;

import com.example.order.model.Order;
import com.example.order.repository.OrderRepository;
import com.example.order.repository.PaymentQueueRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import com.example.order.client.PaymentClient;
import com.example.order.dto.PaymentRequestDto;
import com.example.order.dto.PaymentResponseDto;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Service
public class OrderService {
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);
    
    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentClient paymentClient;

    @Autowired
    private PaymentQueueRepository paymentQueueRepository;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    private RetryRegistry retryRegistry;

    @Autowired
    private Counter ordersCreatedCounter;

    @Autowired
    private Counter ordersFailedCounter;

    @Autowired
    private Counter paymentStatusCounter;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Order createOrder(Order order) {
        logger.info("=== STARTING ORDER CREATION ===");

        // 🔐 EXTRACT CUSTOMER ID FROM JWT TOKEN
        Long customerId = extractCustomerIdFromToken();
        if (customerId == null) {
            logger.error("Unauthorized: Cannot identify customer from token");
            throw new RuntimeException("Unauthorized: Cannot identify customer from token");
        }

        // Set customer ID from token (NOT from request body)
        order.setCustomerId(customerId);
        logger.info("Order being created for customer: {}", customerId);

        // Set initial status
        order.setPaymentStatus("PENDING");
        
        try {
            Order savedOrder = orderRepository.save(order);
            ordersCreatedCounter.increment();
            
            logger.info("Order saved successfully: ID={}, Product={}, Customer={}", 
                       savedOrder.getId(), savedOrder.getProductName(), savedOrder.getCustomerId());

            // If payment method provided, call Payment Service with resilience patterns
            if (savedOrder.getPaymentMethod() != null && !savedOrder.getPaymentMethod().isEmpty()) {
                callPaymentServiceWithResilience(savedOrder);
            }

            return savedOrder;
            
        } catch (Exception e) {
            logger.error("Failed to create order: {}", e.getMessage(), e);
            ordersFailedCounter.increment();
            throw e;
        }
    }

    private Boolean callPaymentServiceWithWebClient(Order order) {
        logger.info("Calling payment service for order ID={}, Amount={}", 
                   order.getId(), order.getPrice() * order.getQuantity());
        
        try {
            // Create payment request
            PaymentRequestDto paymentRequest = new PaymentRequestDto(
                    order.getId().toString(),
                    order.getPrice() * order.getQuantity(),
                    order.getPaymentMethod());

            // Get the current Authorization header
            String authHeader = getCurrentAuthorizationHeader();

            // Call payment service with WebClient
            PaymentResponseDto paymentResponse = paymentClient.processPayment(paymentRequest, authHeader)
                    .block();

            logger.info("Payment response received - Status: {}, Payment ID: {}", 
                       paymentResponse.getStatus(), paymentResponse.getPaymentId());

            // STEP 4: FALLBACK STRATEGY - Check if this is a fallback response
            if ("PENDING_PAYMENT".equals(paymentResponse.getStatus()) ||
                    paymentResponse.getPaymentId().startsWith("FALLBACK-")) {
                logger.warn("FALLBACK: Payment service unavailable, order queued - Order ID: {}", 
                           order.getId());
                order.setPaymentStatus("PENDING_PAYMENT");
                order.setPaymentId(paymentResponse.getPaymentId());
                orderRepository.save(order);
                paymentStatusCounter.increment(); // Track fallback usage
                return false;
            }

            // Normal successful payment
            order.setPaymentStatus(paymentResponse.getStatus());
            order.setPaymentId(paymentResponse.getPaymentId());
            orderRepository.save(order);
            
            logger.info("Payment successful for order ID={}, Status: {}", 
                       order.getId(), paymentResponse.getStatus());
            return true;

        } catch (Exception e) {
            logger.error("WebClient payment call failed for order ID={}: {}", 
                        order.getId(), e.getMessage(), e);
            throw new RuntimeException("Payment service call failed", e);
        }
    }

    // 🔐 Extract customerId from JWT token
    private Long extractCustomerIdFromToken() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                    .getRequestAttributes();
            if (attributes != null) {
                logger.debug("Extracting customer ID from token...");
                
                // Try to get from X-Customer-ID header (if NGINX passes it)
                String customerIdHeader = attributes.getRequest().getHeader("X-Customer-ID");
                if (customerIdHeader != null && !customerIdHeader.isEmpty()) {
                    logger.debug("Extracted customerId from X-Customer-ID header: {}", customerIdHeader);
                    return Long.parseLong(customerIdHeader);
                }

                // Fallback: Extract from JWT token directly
                String authHeader = getCurrentAuthorizationHeader();
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String token = authHeader.substring(7);
                    Long customerId = extractCustomerIdFromJWT(token);
                    if (customerId != null) {
                        logger.debug("Extracted customerId from JWT: {}", customerId);
                        return customerId;
                    } else {
                        logger.warn("Could not extract customerId from JWT token");
                    }
                } else {
                    logger.warn("No valid Authorization header found");
                }
            } else {
                logger.warn("ServletRequestAttributes is null - no request context");
            }
        } catch (Exception e) {
            logger.error("Could not extract customerId from token: {}", e.getMessage(), e);
        }

        logger.error("No customerId found in token or headers");
        return null;
    }

    // 🔐 Extract customerId from JWT token payload
    private Long extractCustomerIdFromJWT(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length == 3) {
                String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
                JsonNode jsonNode = objectMapper.readTree(payload);

                logger.debug("JWT payload extracted, checking for customerId...");
                
                if (jsonNode.has("customerId") && !jsonNode.get("customerId").isNull()) {
                    Long customerId = jsonNode.get("customerId").asLong();
                    logger.debug("Found customerId in JWT: {}", customerId);
                    return customerId;
                }
            }
        } catch (Exception e) {
            logger.error("Error parsing JWT: {}", e.getMessage(), e);
        }
        return null;
    }

    // 🔐 ADDED: Extract roles from JWT token
    private List<String> extractRolesFromToken() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                    .getRequestAttributes();
            if (attributes != null) {
                String authHeader = getCurrentAuthorizationHeader();
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String token = authHeader.substring(7);
                    return extractRolesFromJWT(token);
                }
            }
        } catch (Exception e) {
            logger.warn("Could not extract roles from token: {}", e.getMessage());
        }
        return null;
    }

    // 🔐 ADDED: Extract roles from JWT token payload
    private List<String> extractRolesFromJWT(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length == 3) {
                String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
                JsonNode jsonNode = objectMapper.readTree(payload);

                if (jsonNode.has("roles")) {
                    logger.debug("Found roles in JWT token");
                    return objectMapper.convertValue(
                            jsonNode.get("roles"),
                            objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
                }
            }
        } catch (Exception e) {
            logger.error("Error extracting roles from JWT: {}", e.getMessage());
        }
        return null;
    }

    // Helper method to check for admin role with multiple possible names
    private boolean hasAdminRole(List<String> roles) {
        if (roles == null) return false;
        
        boolean isAdmin = roles.stream().anyMatch(role -> role.equals("ROLE_ADMIN") ||
                role.equals("ADMIN") ||
                role.equals("ROLE_ADMINISTRATOR") ||
                role.equals("ADMINISTRATOR"));
        
        logger.debug("User has admin role: {}", isAdmin);
        return isAdmin;
    }

    private void callPaymentServiceWithResilience(Order order) {
        logger.info("Calling payment service with resilience patterns for order ID={}", order.getId());
        
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("paymentService");
        Retry retry = retryRegistry.retry("paymentService");

        Supplier<Boolean> resilientPaymentCall = Retry.decorateSupplier(
                retry,
                CircuitBreaker.decorateSupplier(
                        circuitBreaker,
                        () -> callPaymentServiceWithWebClient(order)));

        try {
            Boolean success = resilientPaymentCall.get();
            if (!success) {
                logger.warn("Payment queued for later processing via fallback - Order ID: {}", order.getId());
            }
        } catch (Exception e) {
            logger.error("All resilience mechanisms failed for order ID={}: {}", 
                        order.getId(), e.getMessage());
            queuePaymentForLater(order);
        }
    }

    private Boolean callPaymentServiceDirect(Order order) {
        try {
            logger.info("Calling payment service directly for order ID={}", order.getId());
            
            String paymentUrl = "http://payment-service:8084/api/payments";

            Map<String, Object> paymentRequest = new HashMap<>();
            paymentRequest.put("orderId", order.getId().toString());
            paymentRequest.put("amount", order.getPrice() * order.getQuantity());
            paymentRequest.put("paymentMethod", order.getPaymentMethod());

            logger.debug("Payment request - URL: {}, Order ID: {}", paymentUrl, paymentRequest.get("orderId"));

            String authHeader = getCurrentAuthorizationHeader();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (authHeader != null) {
                headers.set("Authorization", authHeader);
            }

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(paymentRequest, headers);
            ResponseEntity<Map> responseEntity = restTemplate.exchange(
                    paymentUrl, HttpMethod.POST, requestEntity, Map.class);

            Map<String, Object> paymentResponse = responseEntity.getBody();

            if (paymentResponse != null && paymentResponse.get("status") != null) {
                String status = (String) paymentResponse.get("status");
                String paymentId = (String) paymentResponse.get("paymentId");

                logger.info("Payment successful - Status: {}, Payment ID: {}", status, paymentId);
                
                order.setPaymentStatus(status);
                order.setPaymentId(paymentId);
                orderRepository.save(order);
                return true;
            } else {
                logger.error("Payment response is null or missing status for order ID={}", order.getId());
                order.setPaymentStatus("FAILED");
                orderRepository.save(order);
                return false;
            }

        } catch (Exception e) {
            logger.error("Payment service call failed for order ID={}: {}", 
                        order.getId(), e.getMessage(), e);
            throw new RuntimeException("Payment service call failed", e);
        }
    }

    private void queuePaymentForLater(Order order) {
        logger.warn("Queuing payment for later processing - Order ID: {}", order.getId());
        
        order.setPaymentStatus("PENDING_PAYMENT");
        orderRepository.save(order);

        logger.info("Payment queued for order ID={}, status: PENDING_PAYMENT", order.getId());
    }

    private String getCurrentAuthorizationHeader() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                    .getRequestAttributes();
            if (attributes != null) {
                String authHeader = attributes.getRequest().getHeader("Authorization");
                logger.debug("Authorization header present: {}", authHeader != null);
                return authHeader;
            }
        } catch (Exception e) {
            logger.error("Could not get Authorization header: {}", e.getMessage());
        }
        return null;
    }

    public List<Order> getAllOrders() {
        Long customerId = extractCustomerIdFromToken();
        List<String> roles = extractRolesFromToken();

        logger.info("Getting orders - Roles: {}, Customer ID: {}", roles, customerId);
        
        if (roles != null && hasAdminRole(roles)) {
            logger.info("Admin user - returning ALL orders");
            return orderRepository.findAll();
        }
        if (customerId != null) {
            logger.info("Customer user - returning orders for customer ID: {}", customerId);
            return orderRepository.findByCustomerId(customerId);
        }
        logger.error("Unauthorized: Cannot identify user from token");
        throw new RuntimeException("Unauthorized: Cannot identify user from token");
    }

    public Order getOrder(Long id) {
        Long customerId = extractCustomerIdFromToken();
        List<String> roles = extractRolesFromToken();

        logger.info("Getting order ID={} - Roles: {}, Customer ID: {}", id, roles, customerId);
        
        if (roles != null && hasAdminRole(roles)) {
            logger.info("Admin user - accessing any order");
            return orderRepository.findById(id).orElse(null);
        }
        if (customerId != null) {
            logger.info("Customer user - accessing only customer's order");
            return orderRepository.findByIdAndCustomerId(id, customerId).orElse(null);
        }
        logger.error("Unauthorized: Cannot identify user from token");
        throw new RuntimeException("Unauthorized: Cannot identify user from token");
    }

    public Order updateOrder(Long id, Order order) {
        logger.info("Updating order ID={}", id);
        order.setId(id);
        
        try {
            Order updatedOrder = orderRepository.save(order);
            logger.info("Order updated successfully: ID={}", id);
            return updatedOrder;
        } catch (Exception e) {
            logger.error("Failed to update order ID={}: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    public void deleteOrder(Long id) {
        logger.info("Deleting order ID={}", id);
        
        try {
            orderRepository.deleteById(id);
            logger.info("Order deleted successfully: ID={}", id);
        } catch (Exception e) {
            logger.error("Failed to delete order ID={}: {}", id, e.getMessage(), e);
            throw e;
        }
    }
}