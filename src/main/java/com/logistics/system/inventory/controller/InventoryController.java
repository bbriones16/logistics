package com.logistics.system.inventory.controller;

import com.logistics.system.inventory.dto.InventoryDto;
import com.logistics.system.inventory.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/warehouse/{warehouseId}")
    public List<InventoryDto.StockResponse> getByWarehouse(@PathVariable Long warehouseId) {
        return inventoryService.findByWarehouse(warehouseId);
    }

    @GetMapping("/product/{productId}")
    public List<InventoryDto.StockResponse> getByProduct(@PathVariable Long productId) {
        return inventoryService.findByProduct(productId);
    }

    @GetMapping("/low-stock")
    public List<InventoryDto.StockResponse> getLowStock() {
        return inventoryService.findLowStock();
    }

    @GetMapping("/product/{productId}/movements")
    public Page<InventoryDto.MovementResponse> getMovements(
            @PathVariable Long productId,
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return inventoryService.findMovementsByProduct(productId, pageable);
    }

    @PostMapping("/receive")
    public InventoryDto.StockResponse receive(@Valid @RequestBody InventoryDto.ReceiveRequest request) {
        return inventoryService.receive(request);
    }

    @PostMapping("/pick")
    public InventoryDto.StockResponse pick(@Valid @RequestBody InventoryDto.PickRequest request) {
        return inventoryService.pick(request);
    }

    @PostMapping("/pack")
    public InventoryDto.StockResponse pack(@Valid @RequestBody InventoryDto.PackRequest request) {
        return inventoryService.pack(request);
    }
}
