package com.logistics.system.shipment.repository;

import com.logistics.system.shipment.domain.Shipment;
import com.logistics.system.shipment.domain.ShipmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    Page<Shipment> findAll(Pageable pageable);
    Optional<Shipment> findByOrderId(Long orderId);
    Optional<Shipment> findByTrackingNumber(String trackingNumber);
    boolean existsByOrderId(Long orderId);
    List<Shipment> findByStatus(ShipmentStatus status);
    long countByStatus(ShipmentStatus status);
}
