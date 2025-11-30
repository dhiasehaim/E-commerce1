package com.example.order.repository;

import com.example.order.model.PaymentQueueEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentQueueRepository extends JpaRepository<PaymentQueueEntry, String> {
    List<PaymentQueueEntry> findByStatus(String status);
}
