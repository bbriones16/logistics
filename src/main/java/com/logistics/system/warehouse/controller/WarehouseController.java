package com.logistics.system.warehouse.controller;

import com.logistics.system.warehouse.dto.WarehouseDto;
import com.logistics.system.warehouse.service.WarehouseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/warehouses")
public class WarehouseController {

    private final WarehouseService warehouseService;

    public WarehouseController(WarehouseService warehouseService) {
        this.warehouseService = warehouseService;
    }

    @GetMapping
    public List<WarehouseDto.Response> getAll() {
        return warehouseService.findAll();
    }

    @GetMapping("/{id}")
    public WarehouseDto.Response getById(@PathVariable Long id) {
        return warehouseService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WarehouseDto.Response create(@Valid @RequestBody WarehouseDto.CreateRequest request) {
        return warehouseService.create(request);
    }
}
