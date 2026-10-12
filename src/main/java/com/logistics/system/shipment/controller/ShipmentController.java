package com.logistics.system.shipment.controller;

import com.logistics.system.shipment.dto.ShipmentDto;
import com.logistics.system.shipment.service.ShipmentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shipments")
public class ShipmentController {

    private final ShipmentService shipmentService;

    public ShipmentController(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }

    @GetMapping
    public Page<ShipmentDto.Response> getAll(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return shipmentService.findAll(pageable);
    }

    @GetMapping("/{id}")
    public ShipmentDto.Response getById(@PathVariable Long id) {
        return shipmentService.findById(id);
    }

    @GetMapping("/tracking/{trackingNumber}")
    public ShipmentDto.Response getByTrackingNumber(@PathVariable String trackingNumber) {
        return shipmentService.findByTrackingNumber(trackingNumber);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ShipmentDto.Response create(@Valid @RequestBody ShipmentDto.CreateRequest request) {
        return shipmentService.create(request);
    }

    @PutMapping("/{id}/status")
    public ShipmentDto.Response updateStatus(@PathVariable Long id,
                                             @Valid @RequestBody ShipmentDto.UpdateStatusRequest request) {
        return shipmentService.updateStatus(id, request);
    }
}
