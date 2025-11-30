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

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Order createOrder(Order order) {
        System.out.println("=== ORDER SERVICE: STARTING ORDER CREATION ===");

        // 🔐 EXTRACT CUSTOMER ID FROM JWT TOKEN
        Long customerId = extractCustomerIdFromToken();
        if (customerId == null) {
            throw new RuntimeException("Unauthorized: Cannot identify customer from token");
        }

        // Set customer ID from token (NOT from request body)
        order.setCustomerId(customerId);
        System.out.println(" Order being created for customer: " + customerId);

        // Set initial status
        order.setPaymentStatus("PENDING");
        Order savedOrder = orderRepository.save(order);
        System.out.println(" Order saved with ID: " + savedOrder.getId() + " for customer: " + customerId);

        // If payment method provided, call Payment Service with resilience patterns
        if (savedOrder.getPaymentMethod() != null && !savedOrder.getPaymentMethod().isEmpty()) {
            callPaymentServiceWithResilience(savedOrder);
        }

        return savedOrder;
    }

    private Boolean callPaymentServiceWithWebClient(Order order) {
        try {
            System.out.println("=== CALLING PAYMENT SERVICE (WITH WEBCLIENT + TIMEOUT + FALLBACK) ===");

            // Create payment request
            PaymentRequestDto paymentRequest = new PaymentRequestDto(
                    order.getId().toString(),
                    order.getPrice() * order.getQuantity(),
                    order.getPaymentMethod());

            // Get the current Authorization header
            String authHeader = getCurrentAuthorizationHeader();

            // Call payment service with WebClient (has 5s timeout + fallback built in)
            PaymentResponseDto paymentResponse = paymentClient.processPayment(paymentRequest, authHeader)
                    .block(); // Block for synchronous operation

            System.out.println(" PAYMENT RESPONSE RECEIVED:");
            System.out.println("   - Status: " + paymentResponse.getStatus());
            System.out.println("   - Payment ID: " + paymentResponse.getPaymentId());

            // STEP 4: FALLBACK STRATEGY - Check if this is a fallback response
            if ("PENDING_PAYMENT".equals(paymentResponse.getStatus()) ||
                    paymentResponse.getPaymentId().startsWith("FALLBACK-")) {
                System.out.println(" FALLBACK: Payment service unavailable, order will be queued");
                order.setPaymentStatus("PENDING_PAYMENT");
                order.setPaymentId(paymentResponse.getPaymentId());
                orderRepository.save(order);
                return false; // Indicate fallback was used
            }

            // Normal successful payment
            order.setPaymentStatus(paymentResponse.getStatus());
            order.setPaymentId(paymentResponse.getPaymentId());
            orderRepository.save(order);

            System.out.println("Order updated with payment status: " + paymentResponse.getStatus());
            return true;

        } catch (Exception e) {
            System.err.println("WebClient payment call failed: " + e.getMessage());
            throw new RuntimeException("Payment service call failed", e);
        }
    }

    // 🔐 NEW METHOD: Extract customerId from JWT token
    private Long extractCustomerIdFromToken() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                    .getRequestAttributes();
            if (attributes != null) {
                // 🔍 DEBUG: Log all headers to see what's being received
                System.out.println("=== DEBUG: CHECKING HEADERS IN ORDER SERVICE ===");
                java.util.Enumeration<String> headerNames = attributes.getRequest().getHeaderNames();
                boolean foundAuthHeader = false;
                while (headerNames.hasMoreElements()) {
                    String headerName = headerNames.nextElement();
                    String headerValue = attributes.getRequest().getHeader(headerName);
                    System.out.println("HEADER: " + headerName + " = " + headerValue);
                    if ("Authorization".equalsIgnoreCase(headerName)) {
                        foundAuthHeader = true;
                    }
                }
                System.out.println("Found Authorization header: " + foundAuthHeader);
                System.out.println("=== END DEBUG ===");

                // Try to get from X-Customer-ID header (if NGINX passes it)
                String customerIdHeader = attributes.getRequest().getHeader("X-Customer-ID");
                if (customerIdHeader != null && !customerIdHeader.isEmpty()) {
                    System.out.println("🔐 Extracted customerId from header: " + customerIdHeader);
                    return Long.parseLong(customerIdHeader);
                }

                // Fallback: Extract from JWT token directly
                String authHeader = getCurrentAuthorizationHeader();
                System.out.println("Auth header from getCurrentAuthorizationHeader(): " + authHeader);
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String token = authHeader.substring(7);
                    System.out.println("Token length: " + token.length());
                    Long customerId = extractCustomerIdFromJWT(token);
                    if (customerId != null) {
                        System.out.println("🔐 Extracted customerId from JWT: " + customerId);
                        return customerId;
                    } else {
                        System.err.println(" Could not extract customerId from JWT token");
                    }
                } else {
                    System.err.println(" No valid Authorization header found");
                }
            } else {
                System.err.println(" ServletRequestAttributes is null - no request context");
            }
        } catch (Exception e) {
            System.err.println(" Could not extract customerId from token: " + e.getMessage());
            e.printStackTrace();
        }

        System.err.println(" No customerId found in token or headers");
        return null;
    }

    // 🔐 Extract customerId from JWT token payload
    private Long extractCustomerIdFromJWT(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length == 3) {
                String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
                JsonNode jsonNode = objectMapper.readTree(payload);

                System.out.println("=== JWT PAYLOAD DEBUG ===");
                System.out.println("Full JWT Payload: " + payload);
                System.out.println("Has customerId: " + jsonNode.has("customerId"));
                if (jsonNode.has("customerId")) {
                    System.out.println("customerId value: " + jsonNode.get("customerId"));
                }
                System.out.println("=== END JWT DEBUG ===");

                if (jsonNode.has("customerId") && !jsonNode.get("customerId").isNull()) {
                    return jsonNode.get("customerId").asLong();
                }
            }
        } catch (Exception e) {
            System.err.println(" Error parsing JWT: " + e.getMessage());
            e.printStackTrace();
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
            System.err.println(" Could not extract roles from token: " + e.getMessage());
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
                    return objectMapper.convertValue(
                            jsonNode.get("roles"),
                            objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
                }
            }
        } catch (Exception e) {
            System.err.println(" Error extracting roles from JWT: " + e.getMessage());
        }
        return null;
    }

    // Helper method to check for admin role with multiple possible names
    private boolean hasAdminRole(List<String> roles) {
        return roles.stream().anyMatch(role -> role.equals("ROLE_ADMIN") ||
                role.equals("ADMIN") ||
                role.equals("ROLE_ADMINISTRATOR") ||
                role.equals("ADMINISTRATOR"));
    }

    private void callPaymentServiceWithResilience(Order order) {

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("paymentService");

        Retry retry = retryRegistry.retry("paymentService");


        Supplier<Boolean> resilientPaymentCall = Retry.decorateSupplier(
                retry,
                CircuitBreaker.decorateSupplier(
                        circuitBreaker,
                        () -> callPaymentServiceWithWebClient(order))); // CHANGED: Use WebClient version

        try {
            Boolean success = resilientPaymentCall.get();
            if (!success) {

                System.out.println(" FALLBACK: Payment queued for later processing via fallback");

            }
        } catch (Exception e) {
            System.err.println(" All resilience mechanisms failed: " + e.getMessage());

            queuePaymentForLater(order);
        }
    }

    private Boolean callPaymentServiceDirect(Order order) {
        try {
            System.out.println("=== CALLING PAYMENT SERVICE (WITH RESILIENCE) ===");

            String paymentUrl = "http://payment-service:8084/api/payments";

            Map<String, Object> paymentRequest = new HashMap<>();
            paymentRequest.put("orderId", order.getId().toString());
            paymentRequest.put("amount", order.getPrice() * order.getQuantity());
            paymentRequest.put("paymentMethod", order.getPaymentMethod());

            System.out.println(" Sending to payment service:");
            System.out.println("   - URL: " + paymentUrl);
            System.out.println("   - Order ID: " + paymentRequest.get("orderId"));


            String authHeader = getCurrentAuthorizationHeader();
            System.out.println("   - Auth Header: " + (authHeader != null ? "Present" : "Missing"));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (authHeader != null) {
                headers.set("Authorization", authHeader);
            }

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(paymentRequest, headers);
            ResponseEntity<Map> responseEntity = restTemplate.exchange(
                    paymentUrl, HttpMethod.POST, requestEntity, Map.class);

            Map<String, Object> paymentResponse = responseEntity.getBody();

            System.out.println(" PAYMENT RESPONSE RECEIVED:");
            System.out.println("   - Full Response: " + paymentResponse);
            if (paymentResponse != null && paymentResponse.get("status") != null) {
                String status = (String) paymentResponse.get("status");
                String paymentId = (String) paymentResponse.get("paymentId");

                System.out.println("   - Payment Status: " + status);
                System.out.println("   - Payment ID: " + paymentId);

                order.setPaymentStatus(status);
                order.setPaymentId(paymentId);
                orderRepository.save(order);

                System.out.println(" Order updated with payment status: " + status);
                return true;
            } else {
                System.err.println(" Payment response is null or missing status");
                order.setPaymentStatus("FAILED");
                orderRepository.save(order);
                return false;
            }

        } catch (Exception e) {
            System.err.println(" Payment service call failed: " + e.getMessage());
            throw new RuntimeException("Payment service call failed", e);
        }
    }

    private void queuePaymentForLater(Order order) {
        System.out.println(" FALLBACK: Queuing payment for order " + order.getId() + " for later processing");

        order.setPaymentStatus("PENDING_PAYMENT");
        orderRepository.save(order);

        System.out.println(" Payment queued for later processing, order status: PENDING_PAYMENT");
    }

    private String getCurrentAuthorizationHeader() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                    .getRequestAttributes();
            if (attributes != null) {
                return attributes.getRequest().getHeader("Authorization");
            }
        } catch (Exception e) {
            System.err.println(" Could not get Authorization header: " + e.getMessage());
        }
        return null;
    }

    public List<Order> getAllOrders() {
        Long customerId = extractCustomerIdFromToken();
        List<String> roles = extractRolesFromToken();

        System.out.println(" User roles: " + roles + ", customerId: " + customerId);
        if (roles != null && hasAdminRole(roles)) {
            System.out.println(" ADMIN user - returning ALL orders");
            return orderRepository.findAll();
        }
        if (customerId != null) {
            System.out.println(" CUSTOMER user - returning only customer's orders");
            return orderRepository.findByCustomerId(customerId);
        }
        throw new RuntimeException("Unauthorized: Cannot identify user from token");
    }

    public Order getOrder(Long id) {
        Long customerId = extractCustomerIdFromToken();
        List<String> roles = extractRolesFromToken();

        System.out.println(" User roles: " + roles + ", customerId: " + customerId);
        if (roles != null && hasAdminRole(roles)) {
            System.out.println(" ADMIN user - accessing any order");
            return orderRepository.findById(id).orElse(null);
        }
        if (customerId != null) {
            System.out.println(" CUSTOMER user - accessing only customer's order");
            return orderRepository.findByIdAndCustomerId(id, customerId).orElse(null);
        }
        throw new RuntimeException("Unauthorized: Cannot identify user from token");
    }

    public Order updateOrder(Long id, Order order) {
        order.setId(id);
        return orderRepository.save(order);
    }

    public void deleteOrder(Long id) {
        orderRepository.deleteById(id);
    }
}