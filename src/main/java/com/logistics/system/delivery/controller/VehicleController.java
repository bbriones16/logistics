package com.logistics.system.delivery.controller;

import com.logistics.system.delivery.dto.VehicleDto;
import com.logistics.system.delivery.service.VehicleService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @GetMapping
    public List<VehicleDto.Response> getAll() {
        return vehicleService.findAll();
    }

    @GetMapping("/available")
    public List<VehicleDto.Response> getAvailable() {
        return vehicleService.findAvailable();
    }

    @GetMapping("/{id}")
    public VehicleDto.Response getById(@PathVariable Long id) {
        return vehicleService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VehicleDto.Response create(@RequestBody VehicleDto.CreateRequest request) {
        return vehicleService.create(request);
    }
}
