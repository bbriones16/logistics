package com.logistics.system.delivery.controller;

import com.logistics.system.delivery.dto.DriverDto;
import com.logistics.system.delivery.service.DriverService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/drivers")
public class DriverController {

    private final DriverService driverService;

    public DriverController(DriverService driverService) {
        this.driverService = driverService;
    }

    @GetMapping
    public List<DriverDto.Response> getAll() {
        return driverService.findAll();
    }

    @GetMapping("/available")
    public List<DriverDto.Response> getAvailable() {
        return driverService.findAvailable();
    }

    @GetMapping("/{id}")
    public DriverDto.Response getById(@PathVariable Long id) {
        return driverService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DriverDto.Response create(@RequestBody DriverDto.CreateRequest request) {
        return driverService.create(request);
    }
}
