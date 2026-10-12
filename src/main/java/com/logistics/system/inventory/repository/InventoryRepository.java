package com.logistics.system.inventory.repository;

import com.logistics.system.inventory.domain.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    Optional<Inventory> findByProductIdAndWarehouseId(Long productId, Long warehouseId);
    List<Inventory> findByProductId(Long productId);
    List<Inventory> findByWarehouseId(Long warehouseId);

    @Query("SELECT i FROM Inventory i WHERE (i.quantityOnHand - i.quantityReserved) <= i.product.lowStockThreshold")
    List<Inventory> findLowStock();

    @Query("SELECT COALESCE(SUM(i.quantityOnHand), 0) FROM Inventory i")
    long sumTotalStockOnHand();

    @Query("SELECT COUNT(DISTINCT i.product.id) FROM Inventory i")
    long countDistinctProducts();

    @Query("SELECT COUNT(DISTINCT i.warehouse.id) FROM Inventory i")
    long countDistinctWarehouses();
}
