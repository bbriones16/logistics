package com.logistics.system.delivery.repository;

import com.logistics.system.delivery.domain.Delivery;
import com.logistics.system.delivery.domain.DeliveryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
    Page<Delivery> findAll(Pageable pageable);
    boolean existsByShipmentId(Long shipmentId);
    Optional<Delivery> findByShipmentId(Long shipmentId);
    long countByStatus(DeliveryStatus status);
    long countByScheduledDate(LocalDate date);
}
