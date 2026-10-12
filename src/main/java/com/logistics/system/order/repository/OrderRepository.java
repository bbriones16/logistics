package com.logistics.system.order.repository;

import com.logistics.system.order.domain.Order;
import com.logistics.system.order.domain.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Page<Order> findAll(Pageable pageable);
    List<Order> findByCustomerId(Long customerId);
    List<Order> findByStatus(OrderStatus status);
    long countByStatus(OrderStatus status);
    long countByCreatedAtAfter(Instant since);
}
