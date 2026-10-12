package com.logistics.system.shipment.controller;

import com.logistics.system.shipment.domain.ShipmentMedium;
import com.logistics.system.shipment.dto.CarrierDto;
import com.logistics.system.shipment.service.CarrierService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/carriers")
public class CarrierController {

    private final CarrierService carrierService;

    public CarrierController(CarrierService carrierService) {
        this.carrierService = carrierService;
    }

    @GetMapping
    public List<CarrierDto.Response> getAll(@RequestParam(required = false) ShipmentMedium medium) {
        return medium != null ? carrierService.findByMedium(medium) : carrierService.findAll();
    }

    @GetMapping("/{id}")
    public CarrierDto.Response getById(@PathVariable Long id) {
        return carrierService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CarrierDto.Response create(@Valid @RequestBody CarrierDto.CreateRequest request) {
        return carrierService.create(request);
    }
}
