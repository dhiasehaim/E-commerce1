package com.example.order.repository;
import java.util.List;
import java.util.Optional;
import com.example.order.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByCustomerId(Long customerId);

    Optional<Order> findByIdAndCustomerId(Long id, Long customerId);
}
