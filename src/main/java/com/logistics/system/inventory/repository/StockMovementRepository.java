package com.logistics.system.inventory.repository;

import com.logistics.system.inventory.domain.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    Page<StockMovement> findByProductIdOrderByCreatedAtDesc(Long productId, Pageable pageable);
    Page<StockMovement> findByWarehouseIdOrderByCreatedAtDesc(Long warehouseId, Pageable pageable);
    List<StockMovement> findByReferenceId(Long referenceId);
}
