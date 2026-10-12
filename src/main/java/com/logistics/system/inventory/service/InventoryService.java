package com.logistics.system.inventory.service;

import com.logistics.system.common.exception.BusinessException;
import com.logistics.system.common.exception.ResourceNotFoundException;
import com.logistics.system.inventory.domain.Inventory;
import com.logistics.system.inventory.domain.MovementType;
import com.logistics.system.inventory.domain.StockMovement;
import com.logistics.system.inventory.dto.InventoryDto;
import com.logistics.system.inventory.repository.InventoryRepository;
import com.logistics.system.inventory.repository.StockMovementRepository;
import com.logistics.system.warehouse.service.WarehouseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final InventoryRepository inventoryRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ProductService productService;
    private final WarehouseService warehouseService;

    public InventoryService(InventoryRepository inventoryRepository,
                            StockMovementRepository stockMovementRepository,
                            ProductService productService,
                            WarehouseService warehouseService) {
        this.inventoryRepository = inventoryRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.productService = productService;
        this.warehouseService = warehouseService;
    }

    public List<InventoryDto.StockResponse> findByWarehouse(Long warehouseId) {
        return inventoryRepository.findByWarehouseId(warehouseId).stream().map(this::toStockResponse).toList();
    }

    public List<InventoryDto.StockResponse> findByProduct(Long productId) {
        return inventoryRepository.findByProductId(productId).stream().map(this::toStockResponse).toList();
    }

    public List<InventoryDto.StockResponse> findLowStock() {
        return inventoryRepository.findLowStock().stream().map(this::toStockResponse).toList();
    }

    public Page<InventoryDto.MovementResponse> findMovementsByProduct(Long productId, Pageable pageable) {
        return stockMovementRepository.findByProductIdOrderByCreatedAtDesc(productId, pageable)
                .map(this::toMovementResponse);
    }

    public void validateReservation(String sku, int quantity) {
        inventoryRepository.findAll().stream()
                .filter(inv -> inv.getProduct().getSku().equals(sku))
                .findFirst()
                .ifPresentOrElse(
                        inv -> {
                            if (inv.getAvailableQuantity() < quantity) {
                                throw new BusinessException("INSUFFICIENT_STOCK",
                                        "Insufficient stock for SKU " + sku + ": available=" + inv.getAvailableQuantity() + ", requested=" + quantity);
                            }
                        },
                        () -> { throw new BusinessException("NO_INVENTORY", "No inventory record found for SKU: " + sku); }
                );
    }

    @Transactional
    public InventoryDto.StockResponse receive(InventoryDto.ReceiveRequest request) {
        var product = productService.getProduct(request.productId());
        var warehouse = warehouseService.getWarehouse(request.warehouseId());

        Inventory inventory = inventoryRepository
                .findByProductIdAndWarehouseId(product.getId(), warehouse.getId())
                .orElseGet(() -> {
                    Inventory i = new Inventory();
                    i.setProduct(product);
                    i.setWarehouse(warehouse);
                    return i;
                });

        inventory.setQuantityOnHand(inventory.getQuantityOnHand() + request.quantity());
        Inventory saved = inventoryRepository.save(inventory);

        recordMovement(product.getId(), warehouse.getId(), MovementType.RECEIVE, request.quantity(), null, request.notes());
        log.info("Stock received: productId={}, warehouseId={}, qty={}", product.getId(), warehouse.getId(), request.quantity());
        return toStockResponse(saved);
    }

    @Transactional
    public InventoryDto.StockResponse reserve(Long productId, Long warehouseId, int quantity, Long orderId) {
        Inventory inventory = getInventory(productId, warehouseId);

        if (inventory.getAvailableQuantity() < quantity) {
            throw new BusinessException("INSUFFICIENT_STOCK",
                    "Insufficient stock for product " + productId + ": available=" + inventory.getAvailableQuantity() + ", requested=" + quantity);
        }

        inventory.setQuantityReserved(inventory.getQuantityReserved() + quantity);
        Inventory saved = inventoryRepository.save(inventory);

        recordMovement(productId, warehouseId, MovementType.RESERVE, quantity, orderId, null);
        log.info("Stock reserved: productId={}, warehouseId={}, qty={}, orderId={}", productId, warehouseId, quantity, orderId);
        return toStockResponse(saved);
    }

    @Transactional
    public InventoryDto.StockResponse pick(InventoryDto.PickRequest request) {
        Inventory inventory = getInventory(request.productId(), request.warehouseId());

        if (inventory.getQuantityReserved() < request.quantity()) {
            throw new BusinessException("INSUFFICIENT_RESERVED_STOCK",
                    "Cannot pick more than reserved quantity for product " + request.productId());
        }

        inventory.setQuantityOnHand(inventory.getQuantityOnHand() - request.quantity());
        inventory.setQuantityReserved(inventory.getQuantityReserved() - request.quantity());
        Inventory saved = inventoryRepository.save(inventory);

        recordMovement(request.productId(), request.warehouseId(), MovementType.PICK, request.quantity(), request.orderId(), null);
        log.info("Stock picked: productId={}, warehouseId={}, qty={}, orderId={}", request.productId(), request.warehouseId(), request.quantity(), request.orderId());
        return toStockResponse(saved);
    }

    @Transactional
    public InventoryDto.StockResponse pack(InventoryDto.PackRequest request) {
        Inventory inventory = getInventory(request.productId(), request.warehouseId());
        recordMovement(request.productId(), request.warehouseId(), MovementType.PACK, request.quantity(), request.orderId(), null);
        log.info("Stock packed: productId={}, warehouseId={}, qty={}, orderId={}", request.productId(), request.warehouseId(), request.quantity(), request.orderId());
        return toStockResponse(inventory);
    }

    private Inventory getInventory(Long productId, Long warehouseId) {
        return inventoryRepository.findByProductIdAndWarehouseId(productId, warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Inventory for product " + productId + " in warehouse " + warehouseId, 0L));
    }

    private void recordMovement(Long productId, Long warehouseId, MovementType type, int quantity, Long referenceId, String notes) {
        StockMovement movement = new StockMovement();
        movement.setProduct(productService.getProduct(productId));
        movement.setWarehouse(warehouseService.getWarehouse(warehouseId));
        movement.setMovementType(type);
        movement.setQuantity(quantity);
        movement.setReferenceId(referenceId);
        movement.setNotes(notes);
        stockMovementRepository.save(movement);
    }

    private InventoryDto.StockResponse toStockResponse(Inventory i) {
        return new InventoryDto.StockResponse(
                i.getId(),
                i.getProduct().getId(),
                i.getProduct().getSku(),
                i.getProduct().getName(),
                i.getWarehouse().getId(),
                i.getWarehouse().getName(),
                i.getQuantityOnHand(),
                i.getQuantityReserved(),
                i.getAvailableQuantity()
        );
    }

    private InventoryDto.MovementResponse toMovementResponse(StockMovement m) {
        return new InventoryDto.MovementResponse(
                m.getId(),
                m.getProduct().getId(),
                m.getProduct().getSku(),
                m.getWarehouse().getId(),
                m.getMovementType(),
                m.getQuantity(),
                m.getReferenceId(),
                m.getNotes(),
                m.getCreatedAt()
        );
    }
}
