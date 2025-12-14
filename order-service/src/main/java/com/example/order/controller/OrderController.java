package com.example.order.controller;

import com.example.order.model.Order;
import com.example.order.service.OrderService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);
    
    private final OrderService service;
    private final Counter ordersCreatedCounter;
    private final Counter ordersRetrievedCounter;
    private final Timer orderProcessingTimer;
    private final Counter httpRequestsCounter;

    public OrderController(OrderService service, 
                          Counter ordersCreatedCounter,
                          Counter ordersRetrievedCounter,
                          Timer orderProcessingTimer,
                          Counter httpRequestsCounter) {
        this.service = service;
        this.ordersCreatedCounter = ordersCreatedCounter;
        this.ordersRetrievedCounter = ordersRetrievedCounter;
        this.orderProcessingTimer = orderProcessingTimer;
        this.httpRequestsCounter = httpRequestsCounter;
    }

    @GetMapping
    public ResponseEntity<List<Order>> getAll() {
        httpRequestsCounter.increment();
        logger.info("GET /orders - Retrieving all orders");
        
        try {
            List<Order> orders = service.getAllOrders();
            logger.info("Retrieved {} orders", orders.size());
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            logger.error("Failed to retrieve orders: {}", e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getById(@PathVariable Long id) {
        httpRequestsCounter.increment();
        logger.info("GET /orders/{} - Retrieving order by ID", id);
        
        try {
            Order order = service.getOrder(id);
            ordersRetrievedCounter.increment();
            
            if (order != null) {
                logger.info("Order found: ID={}, Product={}, Quantity={}, Customer={}", 
                           order.getId(), order.getProductName(), order.getQuantity(), order.getCustomerId());
                return ResponseEntity.ok(order);
            } else {
                logger.warn("Order not found: ID={}", id);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Failed to retrieve order ID={}: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    @PostMapping
    public ResponseEntity<Order> create(@RequestBody Order order) {
        httpRequestsCounter.increment();
        logger.info("POST /orders - Creating new order - Product: {}, Quantity: {}, Customer: {}", 
                   order.getProductName(), order.getQuantity(), order.getCustomerId());
        
        Timer.Sample sample = Timer.start();
        
        try {
            Order createdOrder = service.createOrder(order);
            ordersCreatedCounter.increment();
            
            logger.info("Order created successfully: ID={}, Product={}, Status={}, Payment={}", 
                       createdOrder.getId(), 
                       createdOrder.getProductName(),
                       createdOrder.getPaymentStatus(),
                       createdOrder.getPaymentMethod());
            
            return ResponseEntity.ok(createdOrder);
            
        } catch (Exception e) {
            logger.error("Failed to create order: {}", e.getMessage(), e);
            throw e;
        } finally {
            sample.stop(orderProcessingTimer);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Order> update(@PathVariable Long id, @RequestBody Order order) {
        httpRequestsCounter.increment();
        logger.info("PUT /orders/{} - Updating order", id);
        
        try {
            Order updatedOrder = service.updateOrder(id, order);
            
            if (updatedOrder != null) {
                logger.info("Order updated: ID={}, New Status={}, New Payment={}", 
                           updatedOrder.getId(), 
                           updatedOrder.getPaymentStatus(),
                           updatedOrder.getPaymentMethod());
                return ResponseEntity.ok(updatedOrder);
            } else {
                logger.warn("Order not found for update: ID={}", id);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Failed to update order ID={}: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        httpRequestsCounter.increment();
        logger.info("DELETE /orders/{} - Deleting order", id);
        
        try {
            service.deleteOrder(id);
            logger.info("Order deleted: ID={}", id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            logger.error("Failed to delete order ID={}: {}", id, e.getMessage(), e);
            throw e;
        }
    }
}